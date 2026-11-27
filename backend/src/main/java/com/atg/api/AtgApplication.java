package com.atg.api;

import com.atg.api.config.EnvFileLoader;
import com.atg.api.config.FirebaseProjectProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtgApplication {
    public static void main(String[] args) {
        EnvFileLoader.load();
        FirebaseProjectProperties.init();
        SpringApplication.run(AtgApplication.class, args);
    }
}
