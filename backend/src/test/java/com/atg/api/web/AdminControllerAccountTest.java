package com.atg.api.web;

import com.atg.api.config.JwtService;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import com.atg.api.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminControllerAccountTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RealtimeHub realtime = mock(RealtimeHub.class);
    private final JwtService jwt = mock(JwtService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final User admin = new User();
    private AdminController controller;

    @BeforeEach
    void setUp() {
        admin.setId(1L);
        admin.setName("Old Name");
        admin.setEmail("admin@example.com");
        admin.setPhone("123");
        admin.setPasswordHash("old-hash");
        admin.setRole(User.Role.ADMIN);
        when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin.getEmail(), null, List.of()));
        controller = new AdminController(
                users,
                mock(WalletService.class),
                mock(WalletTransactionRepository.class),
                mock(DepositRequestRepository.class),
                mock(WithdrawalRequestRepository.class),
                mock(PurchaseRequestRepository.class),
                mock(SignalRepository.class),
                mock(RoomRepository.class),
                mock(RoomMessageRepository.class),
                mock(CourseRepository.class),
                mock(LessonRepository.class),
                realtime,
                jwt,
                encoder);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updatesAdminProfileAndCredentialsAfterCurrentPasswordCheck() {
        when(encoder.matches("current", "old-hash")).thenReturn(true);
        when(encoder.encode("new-pass")).thenReturn("new-hash");
        when(users.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(jwt.generate("new@example.com", "ADMIN", 1)).thenReturn("new-token");

        Dtos.AuthRes result = controller.updateAccount(new Dtos.AdminAccountReq(
                "current", "New Name", "+263 77", "New@Example.com", "new-pass"));

        assertThat(admin.getName()).isEqualTo("New Name");
        assertThat(admin.getPhone()).isEqualTo("+263 77");
        assertThat(admin.getEmail()).isEqualTo("new@example.com");
        assertThat(admin.getPasswordHash()).isEqualTo("new-hash");
        assertThat(admin.getTokenVersion()).isEqualTo(1);
        assertThat(result.token()).isEqualTo("new-token");
        assertThat(result.user().name()).isEqualTo("New Name");
        verify(users).save(admin);
        verify(realtime).publish(RealtimeHub.USERS, Map.of("userId", 1L, "kind", "profile"));
    }

    @Test
    void refusesAccountChangesWithoutTheCurrentPassword() {
        when(encoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> controller.updateAccount(new Dtos.AdminAccountReq(
                "wrong", "Changed Name", null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(admin.getName()).isEqualTo("Old Name");
    }

    @Test
    void grantsAdminRoleAndInvalidatesTheUserSession() {
        User user = new User();
        user.setId(2L);
        user.setName("New Admin");
        user.setEmail("new-admin@example.com");
        user.setRole(User.Role.USER);
        user.setTokenVersion(4);
        when(users.findById(2L)).thenReturn(Optional.of(user));

        Dtos.UserView result = controller.assignAdmin(2L);

        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(user.getTokenVersion()).isEqualTo(5);
        verify(users).save(user);
        verify(realtime).publish(RealtimeHub.USERS, Map.of("userId", 2L, "kind", "role"));
    }

    @Test
    void doesNotAssignAdminRoleToUnknownUsers() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.assignAdmin(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
