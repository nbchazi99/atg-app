package com.atg.api.config;

import com.atg.api.model.AppSettings;
import com.atg.api.model.User;
import com.atg.api.repo.AppSettingsRepository;
import com.atg.api.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final AppSettingsRepository settings;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, AppSettingsRepository settings,
                      PasswordEncoder encoder) {
        this.users = users;
        this.settings = settings;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (settings.findById(1L).isEmpty()) {
            settings.save(new AppSettings());
        }
        if (users.count() > 0) {
            return;
        }

        String email = System.getenv("ADMIN_EMAIL");
        String password = System.getenv("ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No users exist and ADMIN_EMAIL/ADMIN_PASSWORD are unset - "
                    + "no administrator was created. Register one, then promote it.");
            return;
        }

        User admin = new User();
        admin.setName("ATG Admin");
        admin.setEmail(email.trim().toLowerCase());
        admin.setPhone(System.getenv("ADMIN_PHONE") == null ? "" : System.getenv("ADMIN_PHONE"));
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setPoints(0);
        users.save(admin);
        log.info("Seeded the administrator account from environment.");
    }
}
