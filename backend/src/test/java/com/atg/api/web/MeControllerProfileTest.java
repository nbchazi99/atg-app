package com.atg.api.web;

import com.atg.api.config.JwtService;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MeControllerProfileTest {

    private final UserRepository users = mock(UserRepository.class);
    private final WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
    private final DepositRequestRepository deposits = mock(DepositRequestRepository.class);
    private final WithdrawalRequestRepository withdrawals = mock(WithdrawalRequestRepository.class);
    private final PurchaseRequestRepository purchases = mock(PurchaseRequestRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final RealtimeHub realtime = mock(RealtimeHub.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final MeController controller = new MeController(
            users, transactions, deposits, withdrawals, purchases, enrollments,
            realtime, passwordEncoder, jwtService);
    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(7L);
        user.setName("Trader");
        user.setEmail("trader@example.com");
        user.setPhone("+10000000000");
        user.setPasswordHash("encoded-password");
        user.setPoints(42);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        when(jwtService.generate(anyString(), anyString(), anyInt())).thenReturn("updated-token");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updatesOnlyAllowlistedProfileFields() {
        when(users.findByUsernameIgnoreCase("new_trader")).thenReturn(Optional.empty());

        Dtos.ProfileUpdateView result = controller.update(new Dtos.ProfileUpdateReq(
                " New Trader ", "New_Trader", "+10000000001", "Kenya", null, null));

        assertThat(user.getName()).isEqualTo("New Trader");
        assertThat(user.getUsername()).isEqualTo("new_trader");
        assertThat(user.getPhone()).isEqualTo("+10000000001");
        assertThat(user.getCountry()).isEqualTo("Kenya");
        assertThat(user.getPoints()).isEqualTo(42);
        assertThat(user.getRole()).isEqualTo(User.Role.USER);
        assertThat(result.token()).isEqualTo("updated-token");
        verify(users).save(user);
        verify(realtime).publish(
                RealtimeHub.userScope(user.getId()), java.util.Map.of("kind", "profile"));
        verify(realtime).publish(
                RealtimeHub.ROOMS, java.util.Map.of("userId", user.getId(), "kind", "profile"));
    }

    @Test
    void requiresCurrentPasswordAndIssuesNewTokenWhenEmailChanges() {
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());

        Dtos.ProfileUpdateView result = controller.update(new Dtos.ProfileUpdateReq(
                null, null, null, null, "NEW@example.com", "current-password"));

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(result.token()).isEqualTo("updated-token");
        verify(jwtService).generate("new@example.com", "USER", 1);
        verify(users).save(user);
    }

    @Test
    void rejectsEmailChangesWithoutTheCurrentPassword() {
        assertThatThrownBy(() -> controller.update(new Dtos.ProfileUpdateReq(
                null, null, null, null, "new@example.com", null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(user.getEmail()).isEqualTo("trader@example.com");
        verify(users, never()).save(any(User.class));
    }

    @Test
    void rejectsDuplicateUsernames() {
        User anotherUser = new User();
        anotherUser.setId(8L);
        when(users.findByUsernameIgnoreCase("taken")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> controller.update(new Dtos.ProfileUpdateReq(
                null, "taken", null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already in use");

        verify(users, never()).save(any(User.class));
    }

    @Test
    void rejectsProfileUpdatesWithoutAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> controller.update(new Dtos.ProfileUpdateReq(
                "Name", null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(users, never()).save(any(User.class));
    }
}
