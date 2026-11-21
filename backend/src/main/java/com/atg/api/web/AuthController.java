package com.atg.api.web;

import com.atg.api.config.JwtService;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.UserRepository;
import com.atg.api.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RealtimeHub realtime;
    private final PasswordResetService passwordResets;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                          RealtimeHub realtime, PasswordResetService passwordResets) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.realtime = realtime;
        this.passwordResets = passwordResets;
    }

    @PostMapping("/register")
    public Dtos.AuthRes register(@Valid @RequestBody Dtos.RegisterReq req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(req.email().trim().toLowerCase());
        user.setPhone(req.phone().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(User.Role.USER);
        user.setMembership(User.Membership.NONE);
        users.save(user);
        realtime.publish(RealtimeHub.USERS, java.util.Map.of("userId", user.getId(), "kind", "registered"));
        return new Dtos.AuthRes(
                jwt.generate(user.getEmail(), user.getRole().name(), user.getTokenVersion()),
                Dtos.UserView.of(user));
    }

    @PostMapping("/login")
    public Dtos.AuthRes login(@Valid @RequestBody Dtos.LoginReq req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        return new Dtos.AuthRes(
                jwt.generate(user.getEmail(), user.getRole().name(), user.getTokenVersion()),
                Dtos.UserView.of(user));
    }

    @PostMapping("/password-reset/request")
    public Map<String, String> requestPasswordReset(@Valid @RequestBody Dtos.PasswordResetReq req) {
        passwordResets.requestCode(req.email());
        return Map.of("message", "If an account exists for that email, a reset code has been sent.");
    }

    @PostMapping("/password-reset/confirm")
    public Map<String, String> confirmPasswordReset(
            @Valid @RequestBody Dtos.PasswordResetConfirmReq req) {
        passwordResets.resetPassword(req.email(), req.code(), req.password());
        return Map.of("message", "Password reset. You can now sign in.");
    }
}
