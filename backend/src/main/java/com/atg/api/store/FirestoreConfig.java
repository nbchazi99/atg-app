package com.atg.api.store;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirestoreConfig {

    private static final Logger log = LoggerFactory.getLogger(FirestoreConfig.class);

    @Bean
    public FirebaseApp firebaseApp(@Value("${app.firebase.service-account:}") String serviceAccountPath)
            throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        FirebaseOptions.Builder options = FirebaseOptions.builder();
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            options.setCredentials(GoogleCredentials.fromStream(new FileInputStream(serviceAccountPath)));
            log.info("Firebase initialized from service account {}", serviceAccountPath);
        } else {
            // Cloud Run supplies credentials via the metadata server / GOOGLE_APPLICATION_CREDENTIALS
            options.setCredentials(GoogleCredentials.getApplicationDefault());
            log.info("Firebase initialized from application default credentials");
        }
        return FirebaseApp.initializeApp(options.build());
    }

    @Bean
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }
}
