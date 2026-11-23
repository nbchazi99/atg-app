package com.atg.api.web;

import com.atg.api.model.Signal;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import com.atg.api.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminControllerSignalAudienceTest {

    private final SignalRepository signals = mock(SignalRepository.class);
    private AdminController controller;

    @BeforeEach
    void setUp() {
        when(signals.save(any(Signal.class))).thenAnswer(invocation -> {
            Signal signal = invocation.getArgument(0);
            signal.setId(1L);
            return signal;
        });
        controller = new AdminController(
                mock(UserRepository.class),
                mock(WalletService.class),
                mock(WalletTransactionRepository.class),
                mock(DepositRequestRepository.class),
                mock(WithdrawalRequestRepository.class),
                mock(PurchaseRequestRepository.class),
                signals,
                mock(RoomRepository.class),
                mock(RoomMessageRepository.class),
                mock(CourseRepository.class),
                mock(LessonRepository.class),
                mock(RealtimeHub.class),
                mock(com.atg.api.config.JwtService.class),
                mock(PasswordEncoder.class));
    }

    @Test
    void publishesOnlyWithAnExplicitSupportedAudience() {
        Signal signal = controller.createSignal(signalRequest("deriv vip"));

        assertThat(signal.getRoomLabel()).isEqualTo("DERIV VIP");
    }

    @Test
    void rejectsUnclassifiedOrArbitrarySignalAudiences() {
        assertThatThrownBy(() -> controller.createSignal(signalRequest(null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choose OPEN FLOOR, Deriv VIP, or Weltrade VIP");
        assertThatThrownBy(() -> controller.createSignal(signalRequest("VIP room")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choose OPEN FLOOR, Deriv VIP, or Weltrade VIP");
    }

    private static Dtos.SignalReq signalRequest(String audience) {
        return new Dtos.SignalReq(
                "EURUSD buy", "", "EURUSD", "BUY", "LIVE", audience,
                "1.0800", "1.0700", "1.0900", "M10", 1);
    }
}
