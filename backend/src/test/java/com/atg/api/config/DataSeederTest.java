package com.atg.api.config;

import com.atg.api.model.AppSettings;
import com.atg.api.model.Room;
import com.atg.api.repo.AppSettingsRepository;
import com.atg.api.repo.RoomRepository;
import com.atg.api.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSeederTest {

    @Test
    void migratesTheLegacySevenPercentWithdrawalFeeToTenPercent() {
        AppSettings current = new AppSettings();
        current.setWithdrawFeePct(BigDecimal.valueOf(7));
        current.setWithdrawFeeMigrated(false);
        AppSettingsRepository settings = mock(AppSettingsRepository.class);
        when(settings.findById(1L)).thenReturn(Optional.of(current));
        when(settings.save(any(AppSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataSeeder seeder = seeder(settings);
        seeder.run();

        assertThat(current.getWithdrawFeePct()).isEqualByComparingTo("10");
        assertThat(current.isWithdrawFeeMigrated()).isTrue();
        verify(settings).save(current);
    }

    @Test
    void preservesAnExistingCustomWithdrawalFee() {
        AppSettings current = new AppSettings();
        current.setWithdrawFeePct(BigDecimal.valueOf(8));
        current.setWithdrawFeeMigrated(false);
        AppSettingsRepository settings = mock(AppSettingsRepository.class);
        when(settings.findById(1L)).thenReturn(Optional.of(current));
        when(settings.save(any(AppSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataSeeder seeder = seeder(settings);
        seeder.run();

        assertThat(current.getWithdrawFeePct()).isEqualByComparingTo("8");
        assertThat(current.isWithdrawFeeMigrated()).isTrue();
    }

    private static DataSeeder seeder(AppSettingsRepository settings) {
        UserRepository users = mock(UserRepository.class);
        when(users.count()).thenReturn(1L);
        RoomRepository rooms = mock(RoomRepository.class);
        when(rooms.findAll()).thenReturn(List.of());
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new DataSeeder(users, settings, rooms, mock(PasswordEncoder.class));
    }
}
