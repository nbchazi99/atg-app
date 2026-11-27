package com.atg.api.web;

import com.atg.api.model.SupportConversation;
import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.model.SupportMessage;
import com.atg.api.model.SupportTicket;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class SupportDtos {

    private SupportDtos() {}

    public record ConversationView(
            Long id, String topic, String status, int messageCount,
            LocalDateTime createdAt, LocalDateTime updatedAt, String guestToken) {
        static ConversationView of(SupportConversation row, String guestToken) {
            return new ConversationView(row.getId(), row.getTopic(), row.getStatus().name(),
                    row.getMessageCount(), row.getCreatedAt(), row.getUpdatedAt(), guestToken);
        }
    }

    public record MessageReq(
            @NotBlank @Size(max = 4000) String text,
            String guestToken,
            List<String> attachmentUrls) {}

    public record AiReply(
            String message,
            boolean needsHuman,
            String category,
            String priority,
            double confidence) {}

    public record MessageView(
            Long id, Long conversationId, Long ticketId, Long authorId,
            String authorName, String authorType, String text,
            List<String> attachmentUrls, boolean internalNote, LocalDateTime createdAt) {
        static MessageView of(SupportMessage row) {
            return new MessageView(row.getId(), row.getConversationId(), row.getTicketId(),
                    row.getAuthorId(), row.getAuthorName(), row.getAuthorType().name(),
                    row.getText(), row.getAttachmentUrls(), row.isInternalNote(), row.getCreatedAt());
        }
    }

    public record ChatResult(
            ConversationView conversation, MessageView userMessage,
            MessageView assistantMessage, boolean needsHuman, double confidence,
            TicketView ticket) {}

    public record ConversationDetail(
            ConversationView conversation, List<MessageView> messages) {}

    public record TicketCreateReq(
            String guestToken,
            @NotBlank @Size(max = 120) String contactName,
            @NotBlank @Size(max = 32) String phone,
            @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 32) String preferredContact,
            @NotBlank @Size(max = 40) String category,
            @NotBlank @Size(max = 120) String subject,
            @NotBlank @Size(max = 4000) String description) {}

    public record TicketView(
            Long id, String ticketNumber, Long userId, Long conversationId,
            String contactName, String phone, String email, String preferredContact,
            String category, String subject, String description, String priority,
            String status, Long assignedAdminId, String assignedAdminName,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt) {
        static TicketView of(SupportTicket row) {
            return new TicketView(row.getId(), row.getTicketNumber(), row.getUserId(),
                    row.getConversationId(), row.getContactName(), row.getPhone(),
                    row.getEmail(), row.getPreferredContact(), row.getCategory(),
                    row.getSubject(), row.getDescription(), row.getPriority().name(),
                    row.getStatus().name(), row.getAssignedAdminId(), row.getAssignedAdminName(),
                    row.getCreatedAt(), row.getUpdatedAt(), row.getResolvedAt());
        }
    }

    public record TicketDetail(TicketView ticket, List<MessageView> messages) {}

    public record TicketMessageReq(
            @NotBlank @Size(max = 4000) String text,
            String guestToken,
            List<String> attachmentUrls) {}

    public record GuestTicketAccessReq(@NotBlank String guestToken) {}

    public record AdminReplyReq(
            @NotBlank @Size(max = 4000) String text,
            boolean internalNote) {}

    public record TicketStatusReq(
            String status,
            Long assignedAdminId,
            boolean clearAssignedAdmin,
            String priority) {}

    public record KnowledgeEntryReq(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 60) String category,
            @NotBlank @Size(max = 12000) String content,
            @Size(max = 500) String keywords,
            boolean active) {}

    public record KnowledgeEntryView(
            Long id, String title, String category, String content,
            String keywords, boolean active, Long updatedBy,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        static KnowledgeEntryView of(SupportKnowledgeEntry row) {
            return new KnowledgeEntryView(row.getId(), row.getTitle(), row.getCategory(),
                    row.getContent(), row.getKeywords(), row.isActive(), row.getUpdatedBy(),
                    row.getCreatedAt(), row.getUpdatedAt());
        }
    }
}
