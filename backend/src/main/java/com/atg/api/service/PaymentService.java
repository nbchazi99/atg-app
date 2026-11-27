package com.atg.api.service;

import com.atg.api.model.AppSettings;
import com.atg.api.model.Course;
import com.atg.api.model.Enrollment;
import com.atg.api.model.PaymentTransaction;
import com.atg.api.model.PurchaseRequest;
import com.atg.api.model.User;
import com.atg.api.repo.CourseRepository;
import com.atg.api.repo.EnrollmentRepository;
import com.atg.api.repo.PaymentTransactionRepository;
import com.atg.api.repo.PushTokenRepository;
import com.atg.api.repo.UserRepository;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.web.Dtos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Payment gateway orchestration: creates Dischub orders, verifies them
 * server-to-server and settles purchases exactly once.
 *
 * <p>Trust rules: the browser redirect and the Dischub callback are hints only.
 * Settlement always happens after {@link DischubClient#fetchStatus} confirms
 * success with our own API key and the returned amount/currency match what we
 * stored when the order was created.</p>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(100000);

    private final PaymentTransactionRepository payments;
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final PushTokenRepository pushTokens;
    private final WalletService wallet;
    private final DischubClient dischub;
    private final RealtimeHub realtime;
    private final PushService push;
    private final ObjectMapper json;

    /** Serialises webhook + poll verification per order within this process. */
    private final ConcurrentHashMap<String, Object> orderLocks = new ConcurrentHashMap<>();

    public record Checkout(PaymentTransaction order, String ecocashStatus) {}

    public PaymentService(PaymentTransactionRepository payments, UserRepository users,
                          CourseRepository courses, EnrollmentRepository enrollments,
                          PushTokenRepository pushTokens, WalletService wallet,
                          DischubClient dischub, RealtimeHub realtime, PushService push,
                          ObjectMapper json) {
        this.payments = payments;
        this.users = users;
        this.courses = courses;
        this.enrollments = enrollments;
        this.pushTokens = pushTokens;
        this.wallet = wallet;
        this.dischub = dischub;
        this.realtime = realtime;
        this.push = push;
        this.json = json;
    }

    // ------------------------------------------------------------------ create

    public Checkout createIntent(User user, Dtos.PaymentIntentReq req) {
        PaymentTransaction.Type type;
        try {
            type = PaymentTransaction.Type.valueOf(req.type().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw bad("Unknown purchase type");
        }
        String sender = req.senderPhone() == null ? "" : req.senderPhone().trim();
        if (sender.isEmpty()) throw bad("A mobile number is required to receive the payment prompt");

        BigDecimal amount;
        Long courseId = null;
        String serviceName;
        if (type == PaymentTransaction.Type.COURSE) {
            if (req.courseId() == null) throw bad("Course is required");
            Course course = courses.findById(req.courseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
            if (course.getReviewStatus() != null
                    && course.getReviewStatus() != Course.ReviewStatus.PUBLISHED) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
            }
            if (course.getPrice().signum() == 0) {
                throw bad("This course is free. Enroll directly from its course page.");
            }
            boolean enrolled = enrollments.findByUserIdAndCourseId(user.getId(), course.getId()).isPresent();
            if (enrolled) throw bad("You are already enrolled in this course");
            amount = course.getPrice();
            courseId = course.getId();
            serviceName = "Course: " + course.getTitle();
        } else {
            AppSettings settings = wallet.settings();
            amount = type == PaymentTransaction.Type.VIP_DERIV
                    ? settings.getDerivVipPrice()
                    : settings.getWeltradeVipPrice();
            User.Membership wanted = type == PaymentTransaction.Type.VIP_DERIV
                    ? User.Membership.DERIV_VIP
                    : User.Membership.WELTRADE_VIP;
            if (user.getMembership() == wanted && !membershipExpired(user)) {
                throw bad("You already hold an active " + label(type) + " membership");
            }
            serviceName = label(type) + " membership";
        }
        if (amount.signum() <= 0) throw bad("This item has no price configured");
        if (amount.compareTo(MAX_AMOUNT) >= 0) throw bad("Amount must be less than $100,000");

        // Duplicate-payment protection: only one live order per user and type.
        for (PaymentTransaction open : payments.findByUserId(user.getId())) {
            boolean openStatus = open.getStatus() == PaymentTransaction.Status.CREATED
                    || open.getStatus() == PaymentTransaction.Status.PROCESSING;
            if (openStatus && open.getType() == type) {
                open.setStatus(PaymentTransaction.Status.EXPIRED);
                open.setStatusReason("Superseded by a newer checkout");
                open.setUpdatedAt(now());
                payments.save(open);
            }
        }

        PaymentTransaction order = new PaymentTransaction();
        order.setUserId(user.getId());
        order.setType(type);
        order.setCourseId(courseId);
        order.setServiceName(serviceName);
        order.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        order.setCurrency("USD");
        order.setSender(sender);
        order.setStatus(PaymentTransaction.Status.CREATED);
        order.setCreatedAt(now());
        order = payments.save(order);
        // The provider reference doubles as the receipt reference; keep it short
        // (Dischub allows at most 30 characters).
        order.setOrderId("PMT" + order.getId());
        order = payments.save(order);

        // Non-redirect integration (ecocash.md): the order pushes an EcoCash
        // prompt to the customer's handset and we poll for the result — no
        // redirect/callback fields, no test mode.
        DischubClient.OrderCreated created = dischub.createOrder(
                order.getOrderId(), sender, order.getAmount(), sender);
        if (!created.ok()) {
            order.setStatus(PaymentTransaction.Status.FAILED);
            order.setStatusReason(created.message());
            order.setUpdatedAt(now());
            payments.save(order);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, created.message());
        }

        order.setStatus(PaymentTransaction.Status.PROCESSING);
        order.setStatusReason(null);
        order.setEcocashStatus(created.ecocashStatus());
        order.setUpdatedAt(now());
        order = payments.save(order);
        return new Checkout(order, created.ecocashStatus());
    }

    // ------------------------------------------------------------------- read

    public PaymentTransaction statusFor(User user, String orderId) {
        PaymentTransaction order = require(orderId);
        boolean owner = order.getUserId().equals(user.getId());
        if (!owner && user.getRole() != User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        if (order.getStatus() == PaymentTransaction.Status.CREATED
                || order.getStatus() == PaymentTransaction.Status.PROCESSING
                || order.getStatus() == PaymentTransaction.Status.FAILED) {
            // FAILED is re-checked too: a payment that arrived while our rules
            // were wrong (or a transient gateway error) must still settle.
            order = verify(order);
        }
        return order;
    }

    public List<PaymentTransaction> history(User user) {
        List<PaymentTransaction> mine = payments.findByUserId(user.getId());
        mine.sort(Comparator.comparing(PaymentTransaction::getCreatedAt).reversed());
        return mine;
    }

    public List<PaymentTransaction> adminList(PaymentTransaction.Status status) {
        List<PaymentTransaction> all = status == null ? payments.findAll() : payments.findByStatus(status);
        all.sort(Comparator.comparing(PaymentTransaction::getCreatedAt).reversed());
        return all;
    }

    // ------------------------------------------------------------- settlement

    /**
     * Re-queries the gateway and settles a successful payment. Safe to call
     * from the client poll, the callback and the background sweep: repeated
     * calls never double-apply an entitlement.
     */
    public PaymentTransaction verify(PaymentTransaction order) {
        synchronized (orderLocks.computeIfAbsent(order.getOrderId(), k -> new Object())) {
            PaymentTransaction current = require(order.getOrderId());
            if (current.getStatus() == PaymentTransaction.Status.SUCCESS
                    || current.getStatus() == PaymentTransaction.Status.REFUNDED) {
                return current;
            }

            DischubClient.ProviderStatus st = dischub.fetchStatus(current.getOrderId());
            if (!st.found()) {
                if (st.message() != null && st.message().toLowerCase(Locale.ROOT).contains("not configured")) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, st.message());
                }
                // Unreachable or unknown order: keep the current state, remember why.
                current.setStatusReason(st.message());
                current.setUpdatedAt(now());
                return payments.save(current);
            }
            // Keep Dischub's own payload on the record for the admin view.
            current.setGatewayResponse(st.raw());
            if (st.isPending()) {
                if (current.getStatus() == PaymentTransaction.Status.CREATED) {
                    current.setStatus(PaymentTransaction.Status.PROCESSING);
                }
                current.setStatusReason(null);
                current.setUpdatedAt(now());
                return payments.save(current);
            }
            if (st.isFailed()) {
                return markFailed(current, st.reason() == null ? "Payment failed at the gateway" : st.reason());
            }

            // Success at the gateway — verify the money matches what we asked for.
            //
            // Dischub's `amount` is the GROSS the customer paid, and the merchant
            // chooses who covers the transaction fee (Dischub > Charges Settings):
            //   merchant dissolves  -> customer pays 30.00, reports "30.00"
            //   customer dissolves  -> customer pays 30.60, reports "30.60"
            //   split 50/50         -> customer pays 30.30, reports "30.30"
            // So the rule is "at least the purchase price": it settles under every
            // charge mode while still rejecting a genuine underpayment.
            if (st.currency() != null && !st.currency().equalsIgnoreCase(current.getCurrency())) {
                return markFailed(current, "Currency mismatch (expected " + current.getCurrency() + ")");
            }
            if (st.amount() == null || st.amount().compareTo(current.getAmount()) < 0) {
                return markFailed(current, "Amount mismatch (expected at least $"
                        + current.getAmount().toPlainString() + ", gateway reported "
                        + (st.amount() == null ? "nothing" : "$" + st.amount().toPlainString()) + ")");
            }
            return settle(current, st);
        }
    }

    private PaymentTransaction settle(PaymentTransaction order, DischubClient.ProviderStatus st) {
        User user = users.findById(order.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        PurchaseRequest.Type purchaseType = switch (order.getType()) {
            case COURSE -> PurchaseRequest.Type.COURSE;
            case VIP_DERIV -> PurchaseRequest.Type.VIP_DERIV;
            case VIP_WELTRADE -> PurchaseRequest.Type.VIP_WELTRADE;
        };
        PurchaseRequest purchase = wallet.recordSettledGatewayPurchase(
                user, purchaseType, order.getCourseId(), order.getAmount(), order.getOrderId());

        order.setStatus(PaymentTransaction.Status.SUCCESS);
        order.setStatusReason(null);
        order.setReceiptNo("RCPT-" + order.getOrderId());
        // Keep the gateway's own detail for the admin payment record: what the
        // customer actually paid (gross, including charges), how, and when.
        order.setPaidAmount(st.amount());
        order.setPaymentMethod(st.paymentMethod());
        order.setGatewayTimestamp(st.timestamp());
        order.setPurchaseRequestId(purchase.getId());
        order.setTransactionId(purchase.getTransactionId());
        order.setPaidAt(now());
        order.setUpdatedAt(now());
        order = payments.save(order);

        realtime.publishWalletChange(user.getId(), "payment");
        realtime.publish(RealtimeHub.USERS, Map.of("userId", user.getId(), "kind", "purchase"));
        realtime.publish(RealtimeHub.ROOMS, Map.of("kind", "purchase"));
        realtime.publish(RealtimeHub.COURSES, Map.of("kind", "purchase"));
        notifyUser(user, "Payment confirmed",
                order.getServiceName() + " is now unlocked. Receipt " + order.getReceiptNo() + ".");
        log.info("Settled payment {} for user {} ({})", order.getOrderId(), user.getEmail(), order.getAmount());
        return order;
    }

    private PaymentTransaction markFailed(PaymentTransaction order, String reason) {
        boolean alreadyFailed = order.getStatus() == PaymentTransaction.Status.FAILED;
        order.setStatus(PaymentTransaction.Status.FAILED);
        order.setStatusReason(reason);
        order.setUpdatedAt(now());
        PaymentTransaction saved = payments.save(order);
        if (!alreadyFailed) {
            // Only notify on the transition; background re-checks stay quiet.
            users.findById(saved.getUserId()).ifPresent(user ->
                    notifyUser(user, "Payment failed", saved.getServiceName() + ": " + reason
                            + " You can try again."));
            realtime.publishWalletChange(saved.getUserId(), "payment");
        }
        return saved;
    }

    // --------------------------------------------------------------- callback

    /**
     * Dischub's callback carries no signature, so it is only a nudge to go
     * re-verify with our own API key. Unknown payloads are acknowledged with
     * 200 so the provider does not retry forever.
     */
    public void handleCallback(String body) {
        String orderId = extractOrderId(body);
        if (orderId == null) {
            log.warn("Dischub callback without an order id: {}", truncate(body));
            return;
        }
        payments.findByOrderId(orderId).ifPresent(order -> {
            try {
                verify(order);
            } catch (Exception e) {
                log.warn("Verification triggered by callback failed for {}", orderId, e);
            }
        });
    }

    private String extractOrderId(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = json.readTree(body);
            for (String field : List.of("order_id", "orderId", "reference")) {
                if (node.hasNonNull(field)) return node.get(field).asText();
            }
            if (node.hasNonNull("data")) {
                JsonNode data = node.get("data");
                for (String field : List.of("order_id", "orderId")) {
                    if (data.hasNonNull(field)) return data.get(field).asText();
                }
            }
        } catch (Exception ignored) {
            // not JSON — try form encoding
        }
        for (String part : body.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).equals("order_id")) {
                return java.net.URLDecoder.decode(part.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- refunds

    /** Records a refund. The money itself moves outside the app (Dischub has no refund API). */
    public PaymentTransaction refund(String orderId, String note) {
        PaymentTransaction order = require(orderId);
        if (order.getStatus() == PaymentTransaction.Status.REFUNDED) {
            throw bad("This payment was already refunded");
        }
        if (order.getStatus() != PaymentTransaction.Status.SUCCESS) {
            throw bad("Only completed payments can be refunded");
        }
        order.setStatus(PaymentTransaction.Status.REFUNDED);
        order.setRefundNote(note == null ? "" : note.trim());
        order.setRefundedAt(now());
        order.setUpdatedAt(now());
        PaymentTransaction saved = payments.save(order);
        realtime.publishWalletChange(saved.getUserId(), "payment");
        users.findById(saved.getUserId()).ifPresent(user ->
                notifyUser(user, "Refund recorded",
                        "A refund of $" + saved.getAmount().toPlainString() + " for "
                                + saved.getServiceName() + " was recorded by the administrator."));
        return saved;
    }

    // ------------------------------------------------------------------ sweep

    /**
     * Safety net for missed callbacks and abandoned checkouts: re-verifies
     * open orders and expires stale ones so nothing can sit unconfirmed.
     */
    @Scheduled(fixedDelayString = "PT5M")
    public void sweepOpenOrders() {
        if (!dischub.isConfigured()) return;
        LocalDateTime cutoff = now().minusHours(24);
        for (PaymentTransaction order : payments.findAll()) {
            // One-time backfill: orders settled before the gateway detail was
            // captured get their gross amount / method / raw payload filled in.
            if (order.getStatus() == PaymentTransaction.Status.SUCCESS
                    && (order.getPaidAmount() == null || order.getGatewayResponse() == null)) {
                try {
                    DischubClient.ProviderStatus st = dischub.fetchStatus(order.getOrderId());
                    if (st.found() && st.amount() != null) {
                        order.setPaidAmount(st.amount());
                        order.setPaymentMethod(st.paymentMethod());
                        order.setGatewayTimestamp(st.timestamp());
                        order.setGatewayResponse(st.raw());
                        order.setUpdatedAt(now());
                        payments.save(order);
                        log.info("Backfilled gateway detail for {}", order.getOrderId());
                    }
                } catch (Exception e) {
                    log.warn("Backfill could not verify {}", order.getOrderId(), e);
                }
                continue;
            }
            boolean open = order.getStatus() == PaymentTransaction.Status.CREATED
                    || order.getStatus() == PaymentTransaction.Status.PROCESSING;
            boolean recentlyFailed = order.getStatus() == PaymentTransaction.Status.FAILED
                    && order.getCreatedAt() != null
                    && order.getCreatedAt().isAfter(cutoff);
            if (!open && !recentlyFailed) continue;
            try {
                if (open && order.getCreatedAt() != null && order.getCreatedAt().isBefore(cutoff)) {
                    order.setStatus(PaymentTransaction.Status.EXPIRED);
                    order.setStatusReason("Payment not completed within 24 hours");
                    order.setUpdatedAt(now());
                    payments.save(order);
                    continue;
                }
                verify(order);
            } catch (Exception e) {
                log.warn("Sweep could not verify {}", order.getOrderId(), e);
            }
        }
    }

    // ----------------------------------------------------------------- helpers

    private PaymentTransaction require(String orderId) {
        return payments.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    private boolean membershipExpired(User user) {
        return user.getMembershipExpiresAt() != null
                && user.getMembershipExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private void notifyUser(User user, String title, String body) {
        try {
            List<String> tokens = pushTokens.findByUserId(user.getId()).stream()
                    .map(t -> t.getToken())
                    .filter(t -> t != null && !t.isBlank())
                    .toList();
            push.sendToTokens(tokens, title, body);
        } catch (Exception e) {
            log.warn("Could not push notification to user {}", user.getId(), e);
        }
    }

    private static String label(PaymentTransaction.Type type) {
        return switch (type) {
            case COURSE -> "Course";
            case VIP_DERIV -> "Deriv VIP";
            case VIP_WELTRADE -> "Weltrade VIP";
        };
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String truncate(String value) {
        return value.length() > 200 ? value.substring(0, 200) : value;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
