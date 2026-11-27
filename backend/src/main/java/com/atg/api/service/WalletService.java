package com.atg.api.service;

import com.atg.api.model.*;
import com.atg.api.repo.*;
import com.atg.api.web.Dtos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class WalletService {

    private static final List<String> SUPPORTED_PAYMENT_METHODS = List.of("EcoCash");

    private final AppSettingsRepository settingsRepo;
    private final DepositRequestRepository deposits;
    private final WithdrawalRequestRepository withdrawals;
    private final PurchaseRequestRepository purchases;
    private final WalletTransactionRepository transactions;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;

    public WalletService(AppSettingsRepository settingsRepo, DepositRequestRepository deposits,
                         WithdrawalRequestRepository withdrawals, PurchaseRequestRepository purchases,
                         WalletTransactionRepository transactions, CourseRepository courses,
                         EnrollmentRepository enrollments, UserRepository users) {
        this.settingsRepo = settingsRepo;
        this.deposits = deposits;
        this.withdrawals = withdrawals;
        this.purchases = purchases;
        this.transactions = transactions;
        this.courses = courses;
        this.enrollments = enrollments;
        this.users = users;
    }

    public AppSettings settings() {
        AppSettings settings = settingsRepo.findById(1L).orElseGet(() -> settingsRepo.save(new AppSettings()));
        boolean changed = false;
        if (!List.of("EcoCash").equals(settings.getDepositMethods())) {
            settings.setDepositMethods(List.of("EcoCash"));
            changed = true;
        }
        if (!List.of("EcoCash").equals(settings.getWithdrawalMethods())) {
            settings.setWithdrawalMethods(List.of("EcoCash"));
            changed = true;
        }
        return changed ? settingsRepo.save(settings) : settings;
    }

    public AppSettings updateSettings(Dtos.SettingsReq req) {
        AppSettings s = settings();
        if (req.adminFeePct() != null) s.setAdminFeePct(req.adminFeePct());
        if (req.withdrawFeePct() != null) s.setWithdrawFeePct(req.withdrawFeePct());
        if (req.minDeposit() != null) {
            if (req.minDeposit().compareTo(BigDecimal.valueOf(5)) < 0) {
                throw bad("Minimum deposit cannot be below $5");
            }
            s.setMinDeposit(req.minDeposit());
        }
        if (req.maxDeposit() != null) s.setMaxDeposit(req.maxDeposit());
        if (s.getMaxDeposit().compareTo(s.getMinDeposit()) < 0) {
            throw bad("Maximum deposit cannot be lower than minimum deposit");
        }
        if (req.minWithdraw() != null) {
            if (req.minWithdraw().compareTo(BigDecimal.valueOf(5)) < 0) {
                throw bad("Minimum withdrawal cannot be below $5");
            }
            s.setMinWithdraw(req.minWithdraw());
        }
        if (req.maxWithdraw() != null) s.setMaxWithdraw(req.maxWithdraw());
        if (s.getMaxWithdraw().compareTo(s.getMinWithdraw()) < 0) {
            throw bad("Maximum withdrawal cannot be lower than minimum withdrawal");
        }
        if (req.fact() != null) s.setFact(req.fact());
        if (req.promotionTitle() != null) s.setPromotionTitle(req.promotionTitle());
        if (req.promotionBody() != null) s.setPromotionBody(req.promotionBody());
        if (req.depositMethods() != null) {
            s.setDepositMethods(normalizePaymentMethods(req.depositMethods(), "deposit"));
        }
        if (req.withdrawalMethods() != null) {
            s.setWithdrawalMethods(normalizePaymentMethods(req.withdrawalMethods(), "withdrawal"));
        }
        if (req.derivVipPrice() != null) {
            if (req.derivVipPrice().signum() <= 0) throw bad("Deriv VIP price must be greater than 0");
            s.setDerivVipPrice(scale(req.derivVipPrice()));
        }
        if (req.weltradeVipPrice() != null) {
            if (req.weltradeVipPrice().signum() <= 0) throw bad("Weltrade VIP price must be greater than 0");
            s.setWeltradeVipPrice(scale(req.weltradeVipPrice()));
        }
        if (req.vipExpiryDays() != null) {
            if (req.vipExpiryDays() < 1 || req.vipExpiryDays() > 3650) {
                throw bad("VIP expiry must be between 1 and 3650 days");
            }
            s.setVipExpiryDays(req.vipExpiryDays());
        }
        return settingsRepo.save(s);
    }

    public DepositRequest createDeposit(User user, Dtos.DepositReq req) {
        if (!"Weltrade".equalsIgnoreCase(req.broker().trim())) {
            throw bad("Only Weltrade deposits are supported");
        }
        AppSettings s = settings();
        String wallet = requireEnabledPaymentMethod(req.wallet(), s.getDepositMethods(), "deposit");
        BigDecimal amount = scale(req.amount());
        BigDecimal minimum = s.getMinDeposit().compareTo(BigDecimal.valueOf(5)) < 0
                ? BigDecimal.valueOf(5)
                : s.getMinDeposit();
        if (amount.compareTo(minimum) < 0) {
            throw bad("Minimum deposit is $" + minimum);
        }
        if (amount.compareTo(s.getMaxDeposit()) > 0) {
            throw bad("Maximum deposit is $" + s.getMaxDeposit());
        }
        DepositRequest d = new DepositRequest();
        d.setUserId(user.getId());
        d.setBroker("Weltrade");
        d.setClientId(req.clientId().trim());
        d.setWallet(wallet);
        d.setMobile(req.mobile().trim());
        d.setAmount(amount);
        d.setFeePct(s.getAdminFeePct());
        d.setFee(scale(amount.multiply(s.getAdminFeePct()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        d.setTotalPaid(amount.add(d.getFee()));

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(user.getId());
        tx.setType(WalletTransaction.Type.DEPOSIT);
        tx.setDescription("Deposit to Weltrade");
        tx.setAmount(d.getTotalPaid());
        tx.setStatus(WalletTransaction.Status.PENDING);
        transactions.save(tx);

        d.setTransactionId(tx.getId());
        return deposits.save(d);
    }

    public WithdrawalRequest createWithdrawal(User user, Dtos.WithdrawReq req) {
        AppSettings s = settings();
        BigDecimal amount = scale(req.amount());
        String payout = requireEnabledPaymentMethod(req.payout(), s.getWithdrawalMethods(), "withdrawal");
        if (amount.compareTo(s.getMinWithdraw()) < 0) {
            throw bad("Minimum withdrawal is $" + s.getMinWithdraw());
        }
        if (amount.compareTo(s.getMaxWithdraw()) > 0) {
            throw bad("Maximum withdrawal is $" + s.getMaxWithdraw());
        }
        if (req.proofUrl() == null || req.proofUrl().isBlank()) {
            throw bad("A screenshot of the withdrawal confirmation is required");
        }
        WithdrawalRequest w = new WithdrawalRequest();
        w.setUserId(user.getId());
        w.setPayout(payout);
        w.setMobile(req.mobile());
        w.setAmount(amount);
        w.setFeePct(s.getWithdrawFeePct());
        w.setFee(scale(amount.multiply(s.getWithdrawFeePct()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        w.setReceive(amount.subtract(w.getFee()));
        w.setSourceType(req.sourceType());
        w.setSourceDetail(req.sourceDetail());
        w.setProofUrl(req.proofUrl());

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(user.getId());
        tx.setType(WalletTransaction.Type.WITHDRAWAL);
        tx.setDescription("Withdrawal to " + payout);
        tx.setAmount(w.getReceive());
        tx.setStatus(WalletTransaction.Status.PENDING);
        transactions.save(tx);

        w.setTransactionId(tx.getId());
        return withdrawals.save(w);
    }

    public PurchaseRequest createPurchase(User user, Dtos.PurchaseReq req) {
        PurchaseRequest.Type type;
        try {
            type = PurchaseRequest.Type.valueOf(req.type().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw bad("Unknown purchase type");
        }
        BigDecimal amount;
        Long courseId = null;
        String description;
        if (type == PurchaseRequest.Type.COURSE) {
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
            amount = course.getPrice();
            courseId = course.getId();
            description = "Course: " + course.getTitle();
        } else {
            AppSettings s = settings();
            amount = type == PurchaseRequest.Type.VIP_DERIV ? s.getDerivVipPrice() : s.getWeltradeVipPrice();
            description = type == PurchaseRequest.Type.VIP_DERIV ? "Deriv VIP membership" : "Weltrade VIP membership";
        }
        if (req.proofUrl() == null || req.proofUrl().isBlank()) {
            throw bad("A proof of payment screenshot is required");
        }
        PurchaseRequest p = new PurchaseRequest();
        p.setUserId(user.getId());
        p.setType(type);
        p.setCourseId(courseId);
        p.setAmount(amount);
        p.setProofUrl(req.proofUrl());

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(user.getId());
        tx.setType(WalletTransaction.Type.PURCHASE);
        tx.setDescription(description);
        tx.setAmount(amount);
        tx.setStatus(WalletTransaction.Status.PENDING);
        transactions.save(tx);

        p.setTransactionId(tx.getId());
        return purchases.save(p);
    }

    public DepositRequest decideDeposit(Long id, boolean approve, String note) {
        DepositRequest d = deposits.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found"));
        if (d.getStatus() != DepositRequest.Status.PENDING) throw bad("This request was already reviewed");
        d.setStatus(approve ? DepositRequest.Status.APPROVED : DepositRequest.Status.REJECTED);
        d.setAdminNote(note);
        d.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        deposits.save(d);
        settle(d.getTransactionId(), approve);
        return d;
    }

    public WithdrawalRequest decideWithdrawal(Long id, boolean approve, String note) {
        WithdrawalRequest w = withdrawals.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdrawal not found"));
        if (w.getStatus() != WithdrawalRequest.Status.PENDING) throw bad("This request was already reviewed");
        w.setStatus(approve ? WithdrawalRequest.Status.APPROVED : WithdrawalRequest.Status.REJECTED);
        w.setAdminNote(note);
        w.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        withdrawals.save(w);
        settle(w.getTransactionId(), approve);
        return w;
    }

    public PurchaseRequest decidePurchase(Long id, boolean approve, String note) {
        PurchaseRequest p = purchases.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found"));
        if (p.getStatus() != PurchaseRequest.Status.PENDING) throw bad("This request was already reviewed");
        p.setStatus(approve ? PurchaseRequest.Status.APPROVED : PurchaseRequest.Status.REJECTED);
        p.setAdminNote(note);
        p.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        purchases.save(p);

        if (approve) {
            applyApprovedPurchase(p);
        }
        settle(p.getTransactionId(), approve);
        return p;
    }

    /**
     * Applies an approved purchase: enrols the course or grants the VIP
     * membership with an expiry. Used by both the admin decision path and the
     * payment gateway settlement path so entitlement rules live in one place.
     */
    public void applyApprovedPurchase(PurchaseRequest p) {
        User user = users.findById(p.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (p.getType() == PurchaseRequest.Type.COURSE) {
            Enrollment e = enrollments.findByUserIdAndCourseId(user.getId(), p.getCourseId())
                    .orElseGet(Enrollment::new);
            if (e.getId() == null) {
                e.setUserId(user.getId());
                e.setCourseId(p.getCourseId());
                enrollments.save(e);
            }
        } else if (p.getType() == PurchaseRequest.Type.VIP_DERIV) {
            user.setMembership(User.Membership.DERIV_VIP);
            user.setMembershipExpiresAt(vipExpiryDate());
            users.save(user);
        } else {
            user.setMembership(User.Membership.WELTRADE_VIP);
            user.setMembershipExpiresAt(vipExpiryDate());
            users.save(user);
        }
    }

    /**
     * Settles a gateway-verified purchase: writes the purchase record as
     * already approved, settles its wallet transaction and applies the
     * entitlement. Idempotent per gateway order id.
     */
    public PurchaseRequest recordSettledGatewayPurchase(User user, PurchaseRequest.Type type,
                                                        Long courseId, BigDecimal amount, String orderId) {
        var existing = purchases.findByPaymentOrderId(orderId);
        if (existing.isPresent()) return existing.get();

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(user.getId());
        tx.setType(WalletTransaction.Type.PURCHASE);
        tx.setDescription(type == PurchaseRequest.Type.COURSE
                ? "Course purchase"
                : (type == PurchaseRequest.Type.VIP_DERIV ? "Deriv VIP membership" : "Weltrade VIP membership"));
        tx.setAmount(amount);
        tx.setStatus(WalletTransaction.Status.COMPLETED);
        transactions.save(tx);

        PurchaseRequest p = new PurchaseRequest();
        p.setUserId(user.getId());
        p.setType(type);
        p.setCourseId(courseId);
        p.setAmount(amount);
        p.setProofUrl("dischub:" + orderId);
        p.setStatus(PurchaseRequest.Status.APPROVED);
        p.setTransactionId(tx.getId());
        p.setPaymentOrderId(orderId);
        p.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));
        p.setAdminNote("Paid via Dischub order " + orderId);
        p = purchases.save(p);

        applyApprovedPurchase(p);
        return p;
    }

    private LocalDateTime vipExpiryDate() {
        AppSettings s = settings();
        int days = s.getVipExpiryDays() > 0 ? s.getVipExpiryDays() : 30;
        return LocalDateTime.now(ZoneOffset.UTC).plusDays(days);
    }

    private void settle(Long transactionId, boolean approve) {
        if (transactionId == null) return;
        transactions.findById(transactionId).ifPresent(tx -> {
            tx.setStatus(approve ? WalletTransaction.Status.COMPLETED : WalletTransaction.Status.REJECTED);
            transactions.save(tx);
        });
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> normalizePaymentMethods(List<String> methods, String operation) {
        if (methods.isEmpty()) {
            throw bad("At least one " + operation + " payment method must remain enabled");
        }
        List<String> normalized = new ArrayList<>();
        for (String method : methods) {
            String canonical = supportedPaymentMethod(method);
            if (!normalized.contains(canonical)) normalized.add(canonical);
        }
        return List.copyOf(normalized);
    }

    private String requireEnabledPaymentMethod(String requested, List<String> enabled, String operation) {
        String method = supportedPaymentMethod(requested);
        if (!enabled.contains(method)) {
            throw bad(method + " is not currently enabled for " + operation);
        }
        return method;
    }

    private String supportedPaymentMethod(String requested) {
        if (requested == null || requested.isBlank()) {
            throw bad("Choose a supported payment method");
        }
        return SUPPORTED_PAYMENT_METHODS.stream()
                .filter(method -> method.equalsIgnoreCase(requested.trim()))
                .findFirst()
                .orElseThrow(() -> bad("Choose a supported payment method"));
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
