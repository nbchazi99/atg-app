package com.atg.api.web;

import com.atg.api.model.User;
import com.atg.api.config.JwtService;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController extends BaseController {

    private final WalletTransactionRepository transactions;
    private final DepositRequestRepository deposits;
    private final WithdrawalRequestRepository withdrawals;
    private final PurchaseRequestRepository purchases;
    private final EnrollmentRepository enrollments;
    private final RealtimeHub realtime;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public MeController(UserRepository users, WalletTransactionRepository transactions,
                        DepositRequestRepository deposits, WithdrawalRequestRepository withdrawals,
                        PurchaseRequestRepository purchases, EnrollmentRepository enrollments,
                        RealtimeHub realtime, PasswordEncoder passwordEncoder, JwtService jwtService) {
        super(users);
        this.transactions = transactions;
        this.deposits = deposits;
        this.withdrawals = withdrawals;
        this.purchases = purchases;
        this.enrollments = enrollments;
        this.realtime = realtime;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @GetMapping
    public Dtos.UserView profile() {
        return Dtos.UserView.of(super.me());
    }

    @PutMapping
    public Dtos.ProfileUpdateView update(@Valid @RequestBody Dtos.ProfileUpdateReq request) {
        User user = me();
        boolean updated = false;
        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) throw bad("Full name cannot be empty");
            user.setName(name);
            updated = true;
        }
        if (request.username() != null) {
            String username = request.username().trim();
            if (!username.isEmpty() && !username.matches("[A-Za-z0-9._-]{3,32}")) {
                throw bad("Username must be 3-32 characters using letters, numbers, dots, underscores or hyphens");
            }
            if (!username.isEmpty()) {
                users.findByUsernameIgnoreCase(username)
                        .filter(existing -> !existing.getId().equals(user.getId()))
                        .ifPresent(existing -> { throw bad("That username is already in use"); });
            }
            user.setUsername(username.isEmpty() ? null : username.toLowerCase(Locale.ROOT));
            updated = true;
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
            updated = true;
        }
        if (request.country() != null) {
            String country = request.country().trim();
            user.setCountry(country.isEmpty() ? null : country);
            updated = true;
        }
        boolean emailChanged = request.email() != null
                && !request.email().trim().equalsIgnoreCase(user.getEmail());
        if (request.email() != null && request.email().isBlank()) {
            throw bad("Email cannot be empty");
        }
        if (emailChanged) {
            if (request.currentPassword() == null || user.getPasswordHash() == null
                    || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Enter your current password to change your email");
            }
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            users.findByEmailIgnoreCase(email)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> { throw bad("That email is already in use"); });
            user.setEmail(email);
            user.setTokenVersion(user.getTokenVersion() + 1);
            updated = true;
        }
        if (!updated) {
            throw bad("Update at least one profile field");
        }
        users.save(user);
        realtime.publish(RealtimeHub.USERS, Map.of("userId", user.getId(), "kind", "profile"));
        realtime.publish(RealtimeHub.userScope(user.getId()), Map.of("kind", "profile"));
        realtime.publish(RealtimeHub.ROOMS, Map.of("userId", user.getId(), "kind", "profile"));
        String token = jwtService.generate(user.getEmail(), user.getRole().name(), user.getTokenVersion());
        return Dtos.ProfileUpdateView.of(user, token);
    }

    @GetMapping("/history")
    public Map<String, Object> history() {
        User user = me();
        return Map.of(
                "transactions", transactions.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "deposits", deposits.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "withdrawals", withdrawals.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "purchases", purchases.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "enrollments", enrollments.findByUserId(user.getId())
        );
    }
}
