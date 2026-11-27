package com.atg.api.web;

import com.atg.api.model.Signal;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import com.atg.api.service.PaymentService;
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
                mock(PasswordEncoder.class),
                mock(PaymentService.class));
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

    @Test
    void closingAndReopeningSignalPersistsOutcomeAndHistory() {
        Signal signal = new Signal();
        signal.setId(8L);
        signal.setTitle("EURUSD buy");
        signal.setRoomLabel("OPEN FLOOR");
        signal.setStatus(Signal.Status.LIVE);
        when(signals.findById(8L)).thenReturn(java.util.Optional.of(signal));
        when(signals.save(any(Signal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Signal closed = controller.updateSignal(8L, new Dtos.SignalReq(
                null, null, null, null, "CLOSED", null, null, null, null, null, null,
                "TP_HIT", "1.0920", "Target reached"));

        assertThat(closed.getStatus()).isEqualTo(Signal.Status.CLOSED);
        assertThat(closed.getResult()).isEqualTo("TP_HIT");
        assertThat(closed.getClosingPrice()).isEqualTo("1.0920");
        assertThat(closed.getClosingUpdate()).isEqualTo("Target reached");
        assertThat(closed.getClosedAt()).isNotNull();
        assertThat(closed.getUpdateHistory()).hasSize(1);

        Signal reopened = controller.updateSignal(8L, new Dtos.SignalReq(
                null, null, null, null, "LIVE", null, null, null, null, null, null));
        assertThat(reopened.getStatus()).isEqualTo(Signal.Status.LIVE);
        assertThat(reopened.getResult()).isNull();
        assertThat(reopened.getClosedAt()).isNull();
        assertThat(reopened.getUpdateHistory()).hasSize(2);
    }

    @Test
    void rejectsUnsupportedSignalOutcome() {
        Signal signal = new Signal();
        signal.setId(9L);
        signal.setRoomLabel("OPEN FLOOR");
        when(signals.findById(9L)).thenReturn(java.util.Optional.of(signal));

        assertThatThrownBy(() -> controller.updateSignal(9L, new Dtos.SignalReq(
                null, null, null, null, "CLOSED", null, null, null, null, null, null,
                "WON", null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choose TP Hit, SL Hit, Manual Close, or Other");
    }

    private static Dtos.SignalReq signalRequest(String audience) {
        return new Dtos.SignalReq(
                "EURUSD buy", "", "EURUSD", "BUY", "LIVE", audience,
                "1.0800", "1.0700", "1.0900", "M10", 1);
    }
}
