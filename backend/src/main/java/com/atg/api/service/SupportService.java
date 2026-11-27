package com.atg.api.service;

import com.atg.api.model.SupportConversation;
import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.model.SupportMessage;
import com.atg.api.model.SupportTicket;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.SupportConversationRepository;
import com.atg.api.repo.SupportKnowledgeRepository;
import com.atg.api.repo.SupportMessageRepository;
import com.atg.api.repo.SupportTicketRepository;
import com.atg.api.repo.UserRepository;
import com.atg.api.repo.PushTokenRepository;
import com.atg.api.web.SupportDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SupportService {

    private static final int MAX_MESSAGES_PER_CONVERSATION = 100;
    private static final Set<String> CATEGORIES = Set.of(
            "ACCOUNT", "PAYMENTS", "VIP", "ACADEMY", "SIGNALS", "TECHNICAL", "GENERAL");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SupportConversationRepository conversations;
    private final SupportTicketRepository tickets;
    private final SupportMessageRepository messages;
    private final SupportKnowledgeRepository knowledge;
    private final UserRepository users;
    private final LocalSupportEngine supportEngine;
    private final RealtimeHub realtime;
    private final PushTokenRepository pushTokens;
    private final PushService push;
    private final String storageBucket;

    public SupportService(
            SupportConversationRepository conversations,
            SupportTicketRepository tickets,
            SupportMessageRepository messages,
            SupportKnowledgeRepository knowledge,
            UserRepository users,
            LocalSupportEngine supportEngine,
            RealtimeHub realtime,
            PushTokenRepository pushTokens,
            PushService push,
            @Value("${FIREBASE_STORAGE_BUCKET:}") String storageBucket) {
        this.conversations = conversations;
        this.tickets = tickets;
        this.messages = messages;
        this.knowledge = knowledge;
        this.users = users;
        this.supportEngine = supportEngine;
        this.realtime = realtime;
        this.pushTokens = pushTokens;
        this.push = push;
        this.storageBucket = storageBucket;
    }

    public SupportConversation createConversation(User user) {
        SupportConversation conversation = new SupportConversation();
        conversation.setUserId(user.getId());
        return conversations.save(conversation);
    }

    public GuestConversation createGuestConversation() {
        String token = newGuestToken();
        SupportConversation conversation = new SupportConversation();
        conversation.setGuestTokenHash(hashToken(token));
        conversations.save(conversation);
        return new GuestConversation(conversation, token);
    }

    public List<SupportConversation> listConversations(User user) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    public SupportConversation conversationForUser(Long id, User user) {
        SupportConversation conversation = requireConversation(id);
        requireConversationAccess(conversation, user, null);
        return conversation;
    }

    public List<SupportMessage> conversationMessages(
            Long conversationId, User user, String guestToken, boolean includeInternal) {
        SupportConversation conversation = requireConversation(conversationId);
        requireConversationAccess(conversation, user, guestToken);
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .filter(row -> includeInternal || !row.isInternalNote())
                .toList();
    }

    public ChatTurn sendConversationMessage(
            Long conversationId, User user, String guestToken,
            String text, List<String> attachmentUrls) {
        SupportConversation conversation = requireConversation(conversationId);
        requireConversationAccess(conversation, user, guestToken);
        validateAttachments(attachmentUrls);
        if (conversation.getMessageCount() >= MAX_MESSAGES_PER_CONVERSATION) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "This conversation has reached its message limit.");
        }

        SupportMessage userMessage = new SupportMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setAuthorId(user == null ? null : user.getId());
        userMessage.setAuthorName(user == null ? "Guest" : safeName(user));
        userMessage.setAuthorType(SupportMessage.AuthorType.USER);
        userMessage.setText(text.trim());
        userMessage.setAttachmentUrls(copyAttachments(attachmentUrls));

        List<SupportMessage> history =
                new ArrayList<>(messages.findByConversationIdOrderByCreatedAtAsc(conversationId));
        history.add(userMessage);
        SupportDtos.AiReply answer = supportEngine.answer(
                history.stream()
                        .filter(row -> !row.isInternalNote())
                        .toList());

        messages.save(userMessage);
        SupportMessage assistantMessage = new SupportMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setAuthorName("ATG Support");
        assistantMessage.setAuthorType(SupportMessage.AuthorType.AI);
        assistantMessage.setText(answer.message());
        messages.save(assistantMessage);

        if (conversation.getTopic() == null || conversation.getTopic().isBlank()) {
            conversation.setTopic(topicFrom(text));
        }
        conversation.setMessageCount(conversation.getMessageCount() + 2);
        conversation.setSuggestedCategory(answer.category());
        conversation.setSuggestedPriority(answer.priority());
        if (answer.needsHuman()) conversation.setStatus(SupportConversation.Status.ESCALATED);
        conversation.setUpdatedAt(now());
        conversations.save(conversation);

        SupportTicket ticket = null;
        if (answer.needsHuman()) {
            ticket = user == null
                    ? createGuestEscalationTicket(conversation, answer, text)
                    : createEscalationTicket(conversation, user, answer, text);
        }
        publishChange(conversation, "message");
        return new ChatTurn(conversation, userMessage, assistantMessage,
                answer.needsHuman(), answer.confidence(), ticket);
    }

    public SupportTicket createTicket(
            Long conversationId, User user, String guestToken,
            SupportDtos.TicketCreateReq request) {
        SupportConversation conversation = requireConversation(conversationId);
        requireConversationAccess(conversation, user, guestToken);
        SupportTicket existing = tickets.findByConversationId(conversationId);
        if (existing != null) return existing;

        SupportTicket ticket = new SupportTicket();
        ticket.setConversationId(conversationId);
        ticket.setUserId(user == null ? null : user.getId());
        ticket.setGuestTokenHash(user == null ? conversation.getGuestTokenHash() : null);
        ticket.setContactName(request.contactName().trim());
        ticket.setPhone(request.phone().trim());
        ticket.setEmail(request.email() == null || request.email().isBlank()
                ? null : request.email().trim().toLowerCase(Locale.ROOT));
        ticket.setPreferredContact(normalizePreferredContact(request.preferredContact()));
        ticket.setCategory(normalizeCategory(request.category()));
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(parsePriority(conversation.getSuggestedPriority()));
        ticket.setTicketNumber(ticketNumber());
        ticket.setCreatedAt(now());
        ticket.setUpdatedAt(ticket.getCreatedAt());
        SupportTicket saved = tickets.save(ticket);
        conversation.setStatus(SupportConversation.Status.ESCALATED);
        conversation.setUpdatedAt(now());
        conversations.save(conversation);
        publishChange(conversation, "ticket-created");
        notifyTicketCreated(saved);
        return saved;
    }

    public List<SupportTicket> listTickets(User user) {
        return tickets.findByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    public SupportTicket ticketForUser(Long id, User user, String guestToken) {
        SupportTicket ticket = requireTicket(id);
        requireTicketAccess(ticket, user, guestToken);
        return ticket;
    }

    public List<SupportMessage> ticketMessages(
            SupportTicket ticket, User user, String guestToken, boolean includeInternal) {
        requireTicketAccess(ticket, user, guestToken);
        return messages.findByConversationIdOrderByCreatedAtAsc(ticket.getConversationId()).stream()
                .filter(row -> includeInternal || !row.isInternalNote())
                .toList();
    }

    public SupportMessage addTicketMessage(
            Long ticketId, User user, String guestToken,
            String text, List<String> attachmentUrls) {
        SupportTicket ticket = requireTicket(ticketId);
        requireTicketAccess(ticket, user, guestToken);
        if (ticket.getStatus() == SupportTicket.Status.RESOLVED
                || ticket.getStatus() == SupportTicket.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This ticket is closed.");
        }
        validateAttachments(attachmentUrls);

        SupportMessage message = new SupportMessage();
        message.setConversationId(ticket.getConversationId());
        message.setTicketId(ticket.getId());
        message.setAuthorId(user == null ? null : user.getId());
        message.setAuthorName(user == null ? ticket.getContactName() : safeName(user));
        message.setAuthorType(SupportMessage.AuthorType.USER);
        message.setText(text.trim());
        message.setAttachmentUrls(copyAttachments(attachmentUrls));
        messages.save(message);

        ticket.setStatus(SupportTicket.Status.IN_PROGRESS);
        ticket.setUpdatedAt(now());
        tickets.save(ticket);
        conversations.findById(ticket.getConversationId()).ifPresent(conversation -> {
            conversation.setMessageCount(conversation.getMessageCount() + 1);
            conversation.setUpdatedAt(now());
            conversations.save(conversation);
            publishChange(conversation, "ticket-message");
        });
        return message;
    }

    public SupportTicket resolveTicket(Long id, User user, String guestToken) {
        SupportTicket ticket = requireTicket(id);
        requireTicketAccess(ticket, user, guestToken);
        ticket.setStatus(SupportTicket.Status.RESOLVED);
        ticket.setResolvedAt(now());
        ticket.setUpdatedAt(ticket.getResolvedAt());
        tickets.save(ticket);
        conversations.findById(ticket.getConversationId()).ifPresent(conversation -> {
            conversation.setStatus(SupportConversation.Status.RESOLVED);
            conversation.setUpdatedAt(now());
            conversations.save(conversation);
            publishChange(conversation, "ticket-resolved");
        });
        return ticket;
    }

    public List<SupportTicket> adminTickets() {
        return tickets.findAllByUpdatedAtDesc();
    }

    public SupportTicket adminTicket(Long id) {
        return requireTicket(id);
    }

    public List<SupportMessage> adminTicketMessages(SupportTicket ticket) {
        return messages.findByConversationIdOrderByCreatedAtAsc(ticket.getConversationId());
    }

    public SupportTicket updateTicket(
            Long ticketId, SupportDtos.TicketStatusReq request, User admin) {
        SupportTicket ticket = requireTicket(ticketId);
        if (request.status() != null && !request.status().isBlank()) {
            try {
                ticket.setStatus(SupportTicket.Status.valueOf(
                        request.status().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException error) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown ticket status.");
            }
        }
        if (request.priority() != null && !request.priority().isBlank()) {
            ticket.setPriority(parsePriority(request.priority()));
        }
        if (request.clearAssignedAdmin()) {
            ticket.setAssignedAdminId(null);
            ticket.setAssignedAdminName(null);
        } else if (request.assignedAdminId() != null) {
            User assignee = users.findById(request.assignedAdminId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Assigned administrator not found."));
            if (assignee.getRole() != User.Role.ADMIN) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Tickets can only be assigned to an administrator.");
            }
            ticket.setAssignedAdminId(assignee.getId());
            ticket.setAssignedAdminName(safeName(assignee));
            if (ticket.getStatus() == SupportTicket.Status.OPEN) {
                ticket.setStatus(SupportTicket.Status.ASSIGNED);
            }
        }
        if (ticket.getStatus() == SupportTicket.Status.RESOLVED
                || ticket.getStatus() == SupportTicket.Status.CLOSED) {
            ticket.setResolvedAt(now());
        } else {
            ticket.setResolvedAt(null);
        }
        ticket.setUpdatedAt(now());
        SupportTicket saved = tickets.save(ticket);
        conversations.findById(ticket.getConversationId()).ifPresent(conversation -> {
            conversation.setStatus(ticket.getStatus() == SupportTicket.Status.RESOLVED
                    || ticket.getStatus() == SupportTicket.Status.CLOSED
                    ? SupportConversation.Status.RESOLVED
                    : SupportConversation.Status.ESCALATED);
            conversation.setUpdatedAt(now());
            conversations.save(conversation);
            publishChange(conversation, "ticket-updated");
        });
        return saved;
    }

    public SupportMessage addAdminMessage(
            Long ticketId, User admin, SupportDtos.AdminReplyReq request) {
        SupportTicket ticket = requireTicket(ticketId);
        if (ticket.getStatus() == SupportTicket.Status.RESOLVED
                || ticket.getStatus() == SupportTicket.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reopen the ticket before replying.");
        }
        SupportMessage message = new SupportMessage();
        message.setConversationId(ticket.getConversationId());
        message.setTicketId(ticket.getId());
        message.setAuthorId(admin.getId());
        message.setAuthorName(safeName(admin));
        message.setAuthorType(SupportMessage.AuthorType.ADMIN);
        message.setText(request.text().trim());
        message.setInternalNote(request.internalNote());
        messages.save(message);
        if (!request.internalNote()) {
            ticket.setStatus(SupportTicket.Status.WAITING_FOR_USER);
            ticket.setUpdatedAt(now());
            tickets.save(ticket);
        }
        conversations.findById(ticket.getConversationId()).ifPresent(conversation -> {
            conversation.setMessageCount(conversation.getMessageCount() + 1);
            conversation.setUpdatedAt(now());
            conversations.save(conversation);
            publishChange(conversation, request.internalNote() ? "internal-note" : "admin-reply");
        });
        if (!request.internalNote()) notifyUser(ticket, "ATG support replied",
                "A response is available on your ticket " + ticket.getTicketNumber() + ".");
        return message;
    }

    public List<SupportKnowledgeEntry> knowledgeEntries() {
        return knowledge.findAll();
    }

    public SupportKnowledgeEntry saveKnowledgeEntry(
            Long id, SupportDtos.KnowledgeEntryReq request, User admin) {
        SupportKnowledgeEntry entry;
        if (id == null) {
            entry = new SupportKnowledgeEntry();
            entry.setCreatedAt(now());
        } else {
            entry = knowledge.findById(id).orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Knowledge entry not found."));
        }
        entry.setTitle(request.title().trim());
        entry.setCategory(normalizeCategory(request.category()));
        entry.setContent(request.content().trim());
        entry.setKeywords(request.keywords() == null ? "" : request.keywords().trim());
        entry.setActive(request.active());
        entry.setUpdatedBy(admin.getId());
        entry.setUpdatedAt(now());
        return knowledge.save(entry);
    }

    public void deleteKnowledgeEntry(Long id) {
        if (knowledge.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge entry not found.");
        }
        knowledge.deleteById(id);
    }

    public List<SupportMessage> adminConversationMessages(Long conversationId) {
        requireConversation(conversationId);
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public void validateGuestConversationAttachment(Long conversationId, String guestToken) {
        SupportConversation conversation = requireConversation(conversationId);
        requireConversationAccess(conversation, null, guestToken);
    }

    public void validateUserConversationAttachment(Long conversationId, User user) {
        SupportConversation conversation = requireConversation(conversationId);
        requireConversationAccess(conversation, user, null);
    }

    private SupportTicket createEscalationTicket(
            SupportConversation conversation, User user, SupportDtos.AiReply answer, String text) {
        SupportTicket existing = tickets.findByConversationId(conversation.getId());
        if (existing != null) return existing;
        SupportTicket ticket = new SupportTicket();
        ticket.setConversationId(conversation.getId());
        ticket.setUserId(user.getId());
        ticket.setContactName(safeName(user));
        ticket.setPhone(user.getPhone());
        ticket.setEmail(user.getEmail());
        ticket.setPreferredContact("IN_APP");
        ticket.setCategory(normalizeCategory(answer.category()));
        ticket.setSubject(topicFrom(text));
        ticket.setDescription("The support engine escalated this conversation for administrator follow-up.");
        ticket.setPriority(parsePriority(answer.priority()));
        ticket.setTicketNumber(ticketNumber());
        ticket.setCreatedAt(now());
        ticket.setUpdatedAt(ticket.getCreatedAt());
        SupportTicket saved = tickets.save(ticket);
        publishChange(conversation, "ticket-created");
        notifyTicketCreated(saved);
        return saved;
    }

    private SupportTicket createGuestEscalationTicket(
            SupportConversation conversation, SupportDtos.AiReply answer, String text) {
        SupportTicket existing = tickets.findByConversationId(conversation.getId());
        if (existing != null) return existing;
        SupportTicket ticket = new SupportTicket();
        ticket.setConversationId(conversation.getId());
        ticket.setGuestTokenHash(conversation.getGuestTokenHash());
        ticket.setContactName("Guest");
        ticket.setPreferredContact("IN_APP");
        ticket.setCategory(normalizeCategory(answer.category()));
        ticket.setSubject(topicFrom(text));
        ticket.setDescription("The support engine could not solve this issue with enough confidence. "
                + "The complete conversation is attached to this ticket.");
        ticket.setPriority(parsePriority(answer.priority()));
        ticket.setTicketNumber(ticketNumber());
        ticket.setCreatedAt(now());
        ticket.setUpdatedAt(ticket.getCreatedAt());
        SupportTicket saved = tickets.save(ticket);
        publishChange(conversation, "ticket-created");
        notifyTicketCreated(saved);
        return saved;
    }

    private SupportConversation requireConversation(Long id) {
        return conversations.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Support conversation not found."));
    }

    private SupportTicket requireTicket(Long id) {
        return tickets.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Support ticket not found."));
    }

    private void requireConversationAccess(
            SupportConversation conversation, User user, String guestToken) {
        if (user != null && user.getId().equals(conversation.getUserId())) return;
        if (user == null && tokenMatches(guestToken, conversation.getGuestTokenHash())) return;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support conversation not found.");
    }

    private void requireTicketAccess(SupportTicket ticket, User user, String guestToken) {
        if (user != null && user.getId().equals(ticket.getUserId())) return;
        if (user == null && tokenMatches(guestToken, ticket.getGuestTokenHash())) return;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found.");
    }

    private void validateAttachments(List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        if (urls.size() > 5) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "A message can include up to five attachments.");
        String expectedPrefix = "/v0/b/" + storageBucket + "/o/support-attachments%2F";
        for (String value : urls) {
            try {
                URI uri = URI.create(value);
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || !"firebasestorage.googleapis.com".equalsIgnoreCase(uri.getHost())
                        || uri.getRawPath() == null
                        || !uri.getRawPath().startsWith(expectedPrefix)
                        || value.length() > 2048) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException error) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Attachment URL is not a valid ATG support upload.");
            }
        }
    }

    private static List<String> copyAttachments(List<String> urls) {
        return urls == null ? new ArrayList<>() : new ArrayList<>(urls);
    }

    private static SupportTicket.Priority parsePriority(String value) {
        try {
            return SupportTicket.Priority.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException error) {
            return SupportTicket.Priority.NORMAL;
        }
    }

    private static String normalizeCategory(String value) {
        String category = value == null ? "GENERAL" : value.trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown support category.");
        }
        return category;
    }

    private static String normalizePreferredContact(String value) {
        String contact = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PHONE", "EMAIL", "WHATSAPP", "IN_APP").contains(contact)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown preferred contact method.");
        }
        return contact;
    }

    private static boolean hasContactDetails(User user) {
        return user.getName() != null && !user.getName().isBlank()
                && user.getPhone() != null && !user.getPhone().isBlank();
    }

    private static String safeName(User user) {
        return user.getName() == null || user.getName().isBlank() ? "ATG user" : user.getName().trim();
    }

    private static String topicFrom(String text) {
        String topic = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        return topic.length() <= 80 ? topic : topic.substring(0, 77) + "...";
    }

    private static String ticketNumber() {
        return "ATG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static String newGuestToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required to secure guest support tokens.", error);
        }
    }

    private static boolean tokenMatches(String token, String expectedHash) {
        if (token == null || token.isBlank() || expectedHash == null) return false;
        byte[] actual = hashToken(token).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }

    private void publishChange(SupportConversation conversation, String kind) {
        Map<String, Object> event = java.util.Map.of(
                "conversationId", conversation.getId(), "kind", kind);
        realtime.publish(RealtimeHub.SUPPORT, event);
        if (conversation.getUserId() != null) {
            realtime.publish(RealtimeHub.supportScope(conversation.getUserId()), event);
        }
    }

    private void notifyTicketCreated(SupportTicket ticket) {
        try {
            List<String> adminTokens = users.findAll().stream()
                    .filter(user -> user.getRole() == User.Role.ADMIN)
                    .flatMap(user -> pushTokens.findByUserId(user.getId()).stream())
                    .map(com.atg.api.model.PushToken::getToken)
                    .toList();
            push.sendToTokens(adminTokens, "New ATG support ticket",
                    ticket.getTicketNumber() + " - " + ticket.getSubject());
        } catch (RuntimeException error) {
            // Ticket persistence is authoritative; push delivery is best effort.
        }
    }

    private void notifyUser(SupportTicket ticket, String title, String body) {
        if (ticket.getUserId() == null) return;
        try {
            List<String> tokens = pushTokens.findByUserId(ticket.getUserId()).stream()
                    .map(com.atg.api.model.PushToken::getToken)
                    .toList();
            push.sendToTokens(tokens, title, body);
        } catch (RuntimeException error) {
            // Ticket persistence is authoritative; push delivery is best effort.
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public record GuestConversation(SupportConversation conversation, String token) {}
    public record ChatTurn(
            SupportConversation conversation, SupportMessage userMessage,
            SupportMessage assistantMessage, boolean needsHuman, double confidence,
            SupportTicket ticket) {}
}
