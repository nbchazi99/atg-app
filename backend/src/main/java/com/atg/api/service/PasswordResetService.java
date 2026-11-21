package com.atg.api.service;

import com.atg.api.model.PasswordResetCode;
import com.atg.api.model.User;
import com.atg.api.repo.PasswordResetCodeRepository;
import com.atg.api.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int CODE_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_SECONDS = 60;
    private static final int EXPIRY_MINUTES = 15;

    private final UserRepository users;
    private final PasswordResetCodeRepository resetCodes;
    private final PasswordEncoder passwords;
    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String mailFrom;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            UserRepository users,
            PasswordResetCodeRepository resetCodes,
            PasswordEncoder passwords,
            JavaMailSender mailSender,
            @Value("${app.mail.host:}") String mailHost,
            @Value("${app.mail.from:}") String mailFrom) {
        this.users = users;
        this.resetCodes = resetCodes;
        this.passwords = passwords;
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
    }

    public void requestCode(String email) {
        requireMailConfiguration();
        Optional<User> matchingUser = users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT));
        if (matchingUser.isEmpty()) return;

        User user = matchingUser.get();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PasswordResetCode previous = resetCodes.findById(user.getId()).orElse(null);
        if (previous != null && previous.getRequestedAt() != null
                && now.isBefore(previous.getRequestedAt().plusSeconds(RESEND_SECONDS))) {
            return;
        }

        String code = String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
        PasswordResetCode reset = new PasswordResetCode();
        reset.setId(user.getId());
        reset.setCodeHash(hash(code));
        reset.setRequestedAt(now);
        reset.setExpiresAt(now.plusMinutes(EXPIRY_MINUTES));
        reset.setFailedAttempts(0);
        resetCodes.save(reset);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Your ATG password reset code");
        message.setText("Your ATG password reset code is " + code + ".\n\n"
                + "It expires in " + EXPIRY_MINUTES + " minutes. If you did not request this, "
                + "you can ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException error) {
            restorePreviousCode(user.getId(), previous);
            log.error("Could not send password reset email for user id {}", user.getId(), error);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Could not send reset email. Try again later.", error);
        }
    }

    public void resetPassword(String email, String code, String newPassword) {
        User user = users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(PasswordResetService::invalidCode);
        PasswordResetCode reset = resetCodes.findById(user.getId()).orElse(null);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (reset == null || reset.getCodeHash() == null || reset.getExpiresAt() == null
                || !now.isBefore(reset.getExpiresAt())
                || reset.getFailedAttempts() >= MAX_ATTEMPTS) {
            if (reset != null) resetCodes.deleteById(user.getId());
            throw invalidCode();
        }

        if (!MessageDigest.isEqual(
                reset.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(code).getBytes(StandardCharsets.UTF_8))) {
            reset.setFailedAttempts(reset.getFailedAttempts() + 1);
            if (reset.getFailedAttempts() >= MAX_ATTEMPTS) {
                resetCodes.deleteById(user.getId());
            } else {
                resetCodes.save(reset);
            }
            throw invalidCode();
        }

        resetCodes.deleteById(user.getId());
        user.setPasswordHash(passwords.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);
    }

    private void requireMailConfiguration() {
        if (mailHost.isBlank() || mailFrom.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Password reset email is not configured.");
        }
    }

    private void restorePreviousCode(Long userId, PasswordResetCode previous) {
        if (previous == null) {
            resetCodes.deleteById(userId);
        } else {
            resetCodes.save(previous);
        }
    }

    private static ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset code.");
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
