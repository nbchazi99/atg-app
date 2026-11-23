package com.atg.api.web;

import com.atg.api.model.Signal;
import com.atg.api.model.User;
import com.atg.api.repo.SignalRepository;
import com.atg.api.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignalControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final SignalRepository signals = mock(SignalRepository.class);
    private final User member = new User();
    private SignalController controller;

    @BeforeEach
    void setUp() {
        member.setId(7L);
        member.setName("Trader");
        member.setEmail("trader@example.com");
        member.setRole(User.Role.USER);
        member.setMembership(User.Membership.NONE);
        when(users.findByEmailIgnoreCase(member.getEmail())).thenReturn(Optional.of(member));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member.getEmail(), null, List.of()));
        controller = new SignalController(users, signals);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsOnlyPublicAndMatchingBrokerVipSignals() {
        Signal open = signal("Open floor");
        Signal deriv = signal("Deriv VIP");
        Signal weltrade = signal("Weltrade VIP");
        when(signals.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(open, deriv, weltrade));

        assertThat(controller.list(null, null)).containsExactly(open);

        member.setMembership(User.Membership.DERIV_VIP);
        assertThat(controller.list(null, null)).containsExactly(open, deriv);

        member.setMembership(User.Membership.WELTRADE_VIP);
        assertThat(controller.list(null, null)).containsExactly(open, weltrade);
    }

    private static Signal signal(String roomLabel) {
        Signal signal = new Signal();
        signal.setRoomLabel(roomLabel);
        return signal;
    }
}
