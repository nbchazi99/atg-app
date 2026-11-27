package com.atg.api.config;

import com.atg.api.model.AppSettings;
import com.atg.api.model.Room;
import com.atg.api.model.User;
import com.atg.api.repo.AppSettingsRepository;
import com.atg.api.repo.RoomRepository;
import com.atg.api.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final AppSettingsRepository settings;
    private final RoomRepository rooms;
    private final PasswordEncoder encoder;
    private final Environment env;

    public DataSeeder(UserRepository users, AppSettingsRepository settings, RoomRepository rooms,
                      PasswordEncoder encoder, Environment env) {
        this.users = users;
        this.settings = settings;
        this.rooms = rooms;
        this.encoder = encoder;
        this.env = env;
    }

    @Override
    public void run(String... args) {
        var existingSettings = settings.findById(1L);
        AppSettings currentSettings = existingSettings.orElseGet(AppSettings::new);
        boolean saveSettings = existingSettings.isEmpty();
        if (!List.of("EcoCash").equals(currentSettings.getDepositMethods())) {
            currentSettings.setDepositMethods(List.of("EcoCash"));
            saveSettings = true;
        }
        if (!List.of("EcoCash").equals(currentSettings.getWithdrawalMethods())) {
            currentSettings.setWithdrawalMethods(List.of("EcoCash"));
            saveSettings = true;
        }
        if (!currentSettings.isDepositMinimumMigrated()) {
            if (currentSettings.getMinDeposit() == null
                    || currentSettings.getMinDeposit().compareTo(BigDecimal.valueOf(12.50)) == 0
                    || currentSettings.getMinDeposit().compareTo(BigDecimal.valueOf(5)) < 0) {
                currentSettings.setMinDeposit(BigDecimal.valueOf(5));
            }
            currentSettings.setDepositMinimumMigrated(true);
            saveSettings = true;
        }
        if (!currentSettings.isFinancialLimitsMigrated()) {
            if (currentSettings.getMaxDeposit() == null
                    || currentSettings.getMaxDeposit().compareTo(new BigDecimal("454.54")) == 0) {
                currentSettings.setMaxDeposit(BigDecimal.valueOf(454));
            }
            if (currentSettings.getMinWithdraw() == null
                    || currentSettings.getMinWithdraw().compareTo(new BigDecimal("12.50")) == 0
                    || currentSettings.getMinWithdraw().compareTo(BigDecimal.valueOf(5)) < 0) {
                currentSettings.setMinWithdraw(BigDecimal.valueOf(5));
            }
            if (currentSettings.getMaxWithdraw() == null) {
                currentSettings.setMaxWithdraw(BigDecimal.valueOf(454));
            }
            currentSettings.setFinancialLimitsMigrated(true);
            saveSettings = true;
        }
        if (!currentSettings.isWithdrawFeeMigrated()) {
            if (currentSettings.getWithdrawFeePct() == null
                    || currentSettings.getWithdrawFeePct().compareTo(BigDecimal.valueOf(7)) == 0) {
                currentSettings.setWithdrawFeePct(BigDecimal.valueOf(10));
            }
            currentSettings.setWithdrawFeeMigrated(true);
            saveSettings = true;
        }
        if (saveSettings) settings.save(currentSettings);

        seedRooms();
        if (users.count() > 0) {
            return;
        }

        String email = env.getProperty("ADMIN_EMAIL");
        String password = env.getProperty("ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No users exist and ADMIN_EMAIL/ADMIN_PASSWORD are unset - "
                    + "no administrator was created. Register one, then promote it.");
            return;
        }

        User admin = new User();
        admin.setName("ATG Admin");
        admin.setEmail(email.trim().toLowerCase());
        admin.setPhone(env.getProperty("ADMIN_PHONE", ""));
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setPoints(0);
        users.save(admin);
        log.info("Seeded the administrator account from environment.");
    }

    private void seedRooms() {
        List<Room> existingRooms = new ArrayList<>(rooms.findAll());
        ensureRoom(existingRooms, "Community", "Chat with all signed-in ATG members.");
        ensureRoom(existingRooms, "Deriv VIP", "Private signals and chat for Deriv VIP members.");
        ensureRoom(existingRooms, "Weltrade VIP", "Private signals and chat for Weltrade VIP members.");
    }

    private void ensureRoom(List<Room> existingRooms, String name, String description) {
        boolean exists = existingRooms.stream()
                .anyMatch(room -> name.equalsIgnoreCase(room.getName()));
        if (exists) return;

        Room room = new Room();
        room.setName(name);
        room.setDescription(description);
        room.setPreview("");
        existingRooms.add(rooms.save(room));
    }
}
