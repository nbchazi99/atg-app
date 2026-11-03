package com.atg.api.config;

import com.atg.api.model.AppSettings;
import com.atg.api.model.User;
import com.atg.api.repo.AppSettingsRepository;
import com.atg.api.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

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

        User admin = new User();
        admin.setName("ATG Admin");
        admin.setEmail("admin@atg.app");
        admin.setPhone("+263787322850");
        admin.setPasswordHash(encoder.encode("Admin@123"));
        admin.setRole(User.Role.ADMIN);
        admin.setPoints(0);
        users.save(admin);
    }
}
