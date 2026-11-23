package com.atg.api.service;

import com.atg.api.model.AppSettings;
import com.atg.api.model.User;
import com.atg.api.model.WithdrawalRequest;
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
import static org.mockito.Mockito.verify;
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
        when(settings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
    void acceptsDepositsOnlyForWeltrade() {
        Dtos.DepositReq deriv = new Dtos.DepositReq(
                "Deriv", "client-1", new BigDecimal("5"), "EcoCash", "0712345678");

        assertThatThrownBy(() -> service.createDeposit(user, deriv))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only Weltrade deposits are supported");
    }

    @Test
    void appliesTenPercentWithdrawalFee() {
        WithdrawalRequest request = service.createWithdrawal(user, withdrawal("100"));

        assertThat(request.getFeePct()).isEqualByComparingTo("10");
        assertThat(request.getFee()).isEqualByComparingTo("10.00");
        assertThat(request.getReceive()).isEqualByComparingTo("90.00");
    }

    @Test
    void acceptsOnlyConfiguredDepositAndWithdrawalMethods() {
        assertThat(service.createDeposit(user, deposit("5", "EcoCash"))).isNotNull();
        assertThat(service.createWithdrawal(user, withdrawal("5", "EcoCash"))).isNotNull();
    }

    @Test
    void rejectsDisabledAndUnknownDepositAndWithdrawalMethods() {
        for (String method : List.of("Cash", "SmileCash", "InnBucks")) {
            assertThatThrownBy(() -> service.createDeposit(user, deposit("5", method)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Choose a supported payment method");
            assertThatThrownBy(() -> service.createWithdrawal(user, withdrawal("5", method)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Choose a supported payment method");
        }
    }

    @Test
    void adminCanConfigureOnlyEcoCashForDepositsAndWithdrawals() {
        AppSettings updated = service.updateSettings(
                settingsRequest(List.of("EcoCash"), List.of("ecocash")));

        assertThat(updated.getDepositMethods()).containsExactly("EcoCash");
        assertThat(updated.getWithdrawalMethods()).containsExactly("EcoCash");
        assertThat(service.createDeposit(user, deposit("5")).getWallet()).isEqualTo("EcoCash");
        assertThat(service.createWithdrawal(user, withdrawal("5")).getPayout())
                .isEqualTo("EcoCash");
        assertThatThrownBy(() -> service.updateSettings(
                settingsRequest(List.of("Cash"), List.of("SmileCash"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choose a supported payment method");
    }

    @Test
    void migratesExistingPaymentSettingsToEcoCashOnly() {
        current.setDepositMethods(List.of("Cash", "SmileCash"));
        current.setWithdrawalMethods(List.of("SmileCash"));

        AppSettings migrated = service.settings();

        assertThat(migrated.getDepositMethods()).containsExactly("EcoCash");
        assertThat(migrated.getWithdrawalMethods()).containsExactly("EcoCash");
        verify(settings).save(current);
    }

    @Test
    void rejectsEmptyOrUnsupportedConfiguredMethodLists() {
        assertThatThrownBy(() -> service.updateSettings(settingsRequest(List.of(), null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("At least one deposit payment method");
        assertThatThrownBy(() -> service.updateSettings(settingsRequest(List.of("InnBucks"), null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choose a supported payment method");
    }

    private static Dtos.SettingsReq settingsRequest(
            List<String> depositMethods, List<String> withdrawalMethods) {
        return new Dtos.SettingsReq(
                null, null, null, null, null, null, null, null, null,
                depositMethods, withdrawalMethods);
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
