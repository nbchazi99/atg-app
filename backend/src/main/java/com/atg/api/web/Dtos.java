package com.atg.api.web;

import com.atg.api.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class Dtos {

    private Dtos() {}

    public record RegisterReq(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String phone,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password) {}

    public record LoginReq(@Email @NotBlank String email, @NotBlank String password) {}

    public record PasswordResetReq(@Email @NotBlank String email) {}

    public record PasswordResetConfirmReq(
            @Email @NotBlank String email,
            @NotBlank @jakarta.validation.constraints.Pattern(regexp = "\\d{8}") String code,
            @NotBlank @Size(min = 6, max = 128) String password) {}

    public record AdminAccountReq(
            @NotBlank String currentPassword,
            @Email String email,
            @Size(min = 6, message = "Password must be at least 6 characters") String password) {}

    public record UserView(Long id, String name, String email, String phone, String role,
                           int points, String membership, String createdAt, String avatarUrl) {
        public static UserView of(User u) {
            return new UserView(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                    u.getRole().name(), u.getPoints(), u.getMembership().name(),
                    u.getCreatedAt() == null ? null : u.getCreatedAt().toString(),
                    u.getAvatarUrl());
        }
    }

    public record AuthRes(String token, UserView user) {}

    public record DepositReq(
            @NotBlank String broker,
            @NotBlank String clientId,
            @NotNull BigDecimal amount,
            @NotBlank String wallet,
            @NotBlank String mobile) {}

    public record WithdrawReq(
            @NotBlank String payout,
            String mobile,
            @NotNull BigDecimal amount,
            @NotBlank String sourceType,
            String sourceDetail,
            String proofUrl) {}

    public record PurchaseReq(@NotBlank String type, Long courseId, String proofUrl) {}

    public record MessageReq(
            @Size(max = 2000, message = "Message must be 2000 characters or fewer") String text,
            @Size(max = 2048, message = "Attachment URL is too long") String attachmentUrl,
            @Size(max = 16, message = "Attachment type is too long") String attachmentType) {
        public MessageReq(String text, String attachmentUrl) {
            this(text, attachmentUrl, null);
        }
    }

    public record CommunityThreadReq(
            @NotBlank @Size(max = 100, message = "Title must be 100 characters or fewer") String title,
            @NotBlank @Size(max = 2000, message = "Message must be 2000 characters or fewer") String message) {}

    public record ThreadStateReq(boolean open) {}

    public record RoomReq(@NotBlank String name, String description) {}

    public record DecisionReq(boolean approve, String note) {}

    public record CompleteReq(@NotNull Long lessonId) {}

    public record SignalReq(String title, String subtitle, String symbol, String side, String status,
                            String roomLabel, String entry, String stopLoss, String targets,
                            String ruleName, Integer participants) {}

    public record LessonReq(String title, Integer durationMin, String content, String videoUrl,
                            String materialUrl, String imageUrl) {}

    public record CourseReq(String title, String subtitle, String description, BigDecimal price,
                            String level, String imageUrl, List<LessonReq> lessons) {}

    public record MembershipReq(@NotBlank String membership) {}

    public record SettingsReq(BigDecimal adminFeePct, BigDecimal withdrawFeePct, BigDecimal minDeposit,
                              BigDecimal maxDeposit, BigDecimal minWithdraw, BigDecimal maxWithdraw, String fact,
                              String promotionTitle, String promotionBody, List<String> depositMethods,
                              List<String> withdrawalMethods) {}
}
