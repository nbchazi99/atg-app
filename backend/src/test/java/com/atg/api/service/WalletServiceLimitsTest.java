package com.atg.api.service;

import com.atg.api.model.AppSettings;
import com.atg.api.model.User;
import com.atg.api.repo.AppSettingsRepository;
import com.atg.api.repo.CourseRepository;
import com.atg.api.repo.DepositRequestRepository;
import com.atg.api.repo.EnrollmentRepository;
import com.atg.api.repo.PurchaseRequestRepository;
import com.atg.api.repo.UserRepository;
import com.atg.api.repo.WalletTransactionRepository;
import com.atg.api.repo.WithdrawalRequestRepository;
import com.atg.api.web.Dtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WalletServiceLimitsTest {

    private final AppSettingsRepository settings = mock(AppSettingsRepository.class);
    private final DepositRequestRepository deposits = mock(DepositRequestRepository.class);
    private final WithdrawalRequestRepository withdrawals = mock(WithdrawalRequestRepository.class);
    private final PurchaseRequestRepository purchases = mock(PurchaseRequestRepository.class);
    private final WalletTransactionRepository transactions = mock(WalletTransactionRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final UserRepository users = mock(UserRepository.class);

    private final AppSettings current = new AppSettings();
    private final User user = new User();
    private WalletService service;

    @BeforeEach
    void setUp() {
        when(settings.findById(1L)).thenReturn(Optional.of(current));
        when(withdrawals.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deposits.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new WalletService(
                settings, deposits, withdrawals, purchases, transactions, courses, enrollments, users);
        user.setId(7L);
    }

    @Test
    void acceptsDepositAndWithdrawalAtFiveAndFourHundredFiftyFourDollars() {
        assertThat(service.createDeposit(user, deposit("5")).getAmount())
                .isEqualByComparingTo("5.00");
        assertThat(service.createDeposit(user, deposit("454")).getAmount())
                .isEqualByComparingTo("454.00");
        assertThat(service.createWithdrawal(user, withdrawal("5")).getAmount())
                .isEqualByComparingTo("5.00");
        assertThat(service.createWithdrawal(user, withdrawal("454")).getAmount())
                .isEqualByComparingTo("454.00");
    }

    @Test
    void rejectsAmountsAboveTheMaximums() {
        assertThatThrownBy(() -> service.createDeposit(user, deposit("454.01")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Maximum deposit is $454");
        assertThatThrownBy(() -> service.createWithdrawal(user, withdrawal("454.01")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Maximum withdrawal is $454");
    }

    @Test
    void acceptsOnlyConfiguredDepositAndWithdrawalMethods() {
        for (String method : List.of("EcoCash", "Cash", "SmileCash")) {
            assertThat(service.createDeposit(user, deposit("5", method))).isNotNull();
            assertThat(service.createWithdrawal(user, withdrawal("5", method))).isNotNull();
        }
    }

    @Test
    void rejectsUnsupportedDepositAndWithdrawalMethods() {
        assertThatThrownBy(() -> service.createDeposit(user, deposit("5", "InnBucks")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choose EcoCash, Cash, or SmileCash");
        assertThatThrownBy(() -> service.createWithdrawal(user, withdrawal("5", "Other")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choose EcoCash, Cash, or SmileCash");
    }

    private static Dtos.DepositReq deposit(String amount) {
        return deposit(amount, "EcoCash");
    }

    private static Dtos.DepositReq deposit(String amount, String wallet) {
        return new Dtos.DepositReq(
                "Weltrade", "client-1", new BigDecimal(amount), wallet, "0712345678");
    }

    private static Dtos.WithdrawReq withdrawal(String amount) {
        return withdrawal(amount, "EcoCash");
    }

    private static Dtos.WithdrawReq withdrawal(String amount, String payout) {
        return new Dtos.WithdrawReq(
                payout, "0712345678", new BigDecimal(amount),
                "Weltrade", "client-1", "https://example.com/proof.png");
    }
}
