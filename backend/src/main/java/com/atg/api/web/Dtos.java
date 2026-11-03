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

    public record AdminAccountReq(
            @NotBlank String currentPassword,
            @Email String email,
            @Size(min = 6, message = "Password must be at least 6 characters") String password) {}

    public record UserView(Long id, String name, String email, String phone, String role,
                           int points, String membership, String createdAt) {
        public static UserView of(User u) {
            return new UserView(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                    u.getRole().name(), u.getPoints(), u.getMembership().name(),
                    u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
        }
    }

    public record AuthRes(String token, UserView user) {}

    public record DepositReq(
            @NotBlank String broker,
            String clientId,
            @NotNull BigDecimal amount,
            @NotBlank String wallet,
            String mobile) {}

    public record WithdrawReq(
            @NotBlank String payout,
            String mobile,
            @NotNull BigDecimal amount,
            @NotBlank String sourceType,
            String sourceDetail,
            String proofUrl) {}

    public record PurchaseReq(@NotBlank String type, Long courseId, String proofUrl) {}

    public record MessageReq(@NotBlank String text) {}

    public record DecisionReq(boolean approve, String note) {}

    public record CompleteReq(@NotNull Long lessonId) {}

    public record SignalReq(String title, String subtitle, String symbol, String side, String status,
                            String roomLabel, String entry, String stopLoss, String targets,
                            String ruleName, Integer participants) {}

    public record LessonReq(String title, Integer durationMin, String content, String videoUrl) {}

    public record CourseReq(String title, String subtitle, String description, BigDecimal price,
                            String level, String imageUrl, List<LessonReq> lessons) {}

    public record MembershipReq(@NotBlank String membership) {}

    public record SettingsReq(BigDecimal adminFeePct, BigDecimal withdrawFeePct, BigDecimal minDeposit,
                              BigDecimal maxDeposit, BigDecimal minWithdraw, String fact,
                              String promotionTitle, String promotionBody) {}
}
