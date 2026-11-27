package com.atg.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the Firebase project from whatever service account JSON is present,
 * BEFORE Spring starts, so every {@code @Value} in the context sees one
 * consistent bucket.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>The key path comes from {@code FIREBASE_SERVICE_ACCOUNT} (env or .env),
 *       defaulting to {@code keys/firebase-adminsdk.json} — paste any project's
 *       key there and the backend targets that project.</li>
 *   <li>If {@code FIREBASE_STORAGE_BUCKET} is not configured, it is derived
 *       from the key's {@code project_id} as {@code <project_id>.firebasestorage.app}.</li>
 *   <li>If a bucket IS configured and belongs to a different project, a clear
 *       warning is logged instead of a mysterious upload failure later.</li>
 * </ul>
 */
@Component
public final class FirebaseProjectProperties {

    private static final Logger log = LoggerFactory.getLogger(FirebaseProjectProperties.class);
    private static final String DEFAULT_KEY_PATH = "keys/firebase-adminsdk.json";

    private FirebaseProjectProperties() {
    }

    /** Called from {@code main} before {@code SpringApplication.run}. */
    public static void init() {
        String keyPath = firstNonBlank(
                System.getProperty("FIREBASE_SERVICE_ACCOUNT"),
                System.getenv("FIREBASE_SERVICE_ACCOUNT"),
                DEFAULT_KEY_PATH);
        Path key = Path.of(keyPath);
        if (!Files.isRegularFile(key)) {
            log.info("No service account at {} — Firebase will use Application Default Credentials",
                    key.toAbsolutePath());
            return;
        }
        String projectId;
        try {
            JsonNode json = new ObjectMapper().readTree(Files.readAllBytes(key));
            projectId = json.path("project_id").asText(null);
        } catch (Exception e) {
            log.warn("Could not read project_id from {}: {}", key.toAbsolutePath(), e.getMessage());
            return;
        }
        if (projectId == null || projectId.isBlank()) {
            log.warn("{} has no project_id — cannot derive the storage bucket", key.toAbsolutePath());
            return;
        }

        String configured = firstNonBlank(
                System.getProperty("FIREBASE_STORAGE_BUCKET"),
                System.getenv("FIREBASE_STORAGE_BUCKET"));
        if (configured == null) {
            String derived = projectId + ".firebasestorage.app";
            // Both application.properties and SupportService resolve
            // ${FIREBASE_STORAGE_BUCKET:...}, so one property covers them all.
            System.setProperty("FIREBASE_STORAGE_BUCKET", derived);
            log.info("Firebase project {} — storage bucket derived as {}", projectId, derived);
        } else if (!configured.startsWith(projectId)) {
            log.warn("FIREBASE_STORAGE_BUCKET '{}' belongs to a different project than the "
                            + "service account ('{}'). Uploads will fail — clear the bucket "
                            + "setting to derive it automatically, or use a key for that project.",
                    configured, projectId);
        } else {
            log.info("Firebase project {} — storage bucket {}", projectId, configured);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }
}
