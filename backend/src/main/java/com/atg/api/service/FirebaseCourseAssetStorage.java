package com.atg.api.service;

import com.google.auth.Credentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FirebaseCourseAssetStorage {

    private static final Logger log = LoggerFactory.getLogger(FirebaseCourseAssetStorage.class);
    private static final Credentials EMULATOR_CREDENTIALS = new Credentials() {
        @Override
        public String getAuthenticationType() {
            return "none";
        }

        @Override
        public Map<String, List<String>> getRequestMetadata(URI uri) {
            return Map.of();
        }

        @Override
        public boolean hasRequestMetadata() {
            return false;
        }

        @Override
        public boolean hasRequestMetadataOnly() {
            return true;
        }

        @Override
        public void refresh() {
        }
    };
    private final FirebaseApp app;
    private final String bucketName;

    public FirebaseCourseAssetStorage(
            FirebaseApp app,
            @Value("${app.firebase.storage-bucket:}") String bucketName) {
        this.app = app;
        this.bucketName = bucketName;
    }

    public String store(MultipartFile file, String extension, String contentType) {
        return store(file, "course-assets", extension, contentType);
    }

    public String storeChatAttachment(MultipartFile file, String extension, String contentType) {
        return store(file, "chat-attachments", extension, contentType);
    }

    public String storeSupportAttachment(MultipartFile file, String extension, String contentType) {
        return store(file, "support-attachments", extension, contentType);
    }

    public String storeProfileAvatar(
            MultipartFile file, Long userId, String extension, String contentType) {
        return store(file, "profile-avatars/" + userId, extension, contentType);
    }

    private String store(MultipartFile file, String folder, String extension, String contentType) {
        if (bucketName == null || bucketName.isBlank()) {
            log.error("Firebase Storage bucket is not configured; asset uploads are unavailable");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Firebase Storage is not configured");
        }

        String objectName = folder + "/" + UUID.randomUUID() + extension;
        String downloadToken = UUID.randomUUID().toString();
        Storage storage;
        try {
            storage = storage();
        } catch (IllegalArgumentException | StorageException error) {
            log.error("Firebase Storage bucket {} is unavailable for asset uploads", bucketName, error);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Firebase Storage is unavailable", error);
        }

        BlobInfo blob = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(contentType)
                .setCacheControl("public,max-age=3600")
                .setMetadata(Map.of("firebaseStorageDownloadTokens", downloadToken))
                .build();
        try (InputStream input = file.getInputStream()) {
            storage.createFrom(blob, input);
        } catch (IOException | StorageException error) {
            log.error("Firebase Storage could not save asset {}", objectName, error);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Could not save the file to Firebase Storage", error);
        }
        String encodedObjectName = URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        String emulatorHost = System.getenv("STORAGE_EMULATOR_HOST");
        String downloadHost = emulatorHost == null || emulatorHost.isBlank()
                ? "https://firebasestorage.googleapis.com"
                : (emulatorHost.contains("://") ? emulatorHost : "http://" + emulatorHost);
        return downloadHost + "/v0/b/" + bucketName
                + "/o/" + encodedObjectName + "?alt=media&token=" + downloadToken;
    }

    private Storage storage() {
        String emulatorHost = System.getenv("STORAGE_EMULATOR_HOST");
        if (emulatorHost == null || emulatorHost.isBlank()) {
            return StorageClient.getInstance(app).bucket(bucketName).getStorage();
        }
        String host = emulatorHost.contains("://")
                ? emulatorHost : "http://" + emulatorHost;
        String projectId = app.getOptions().getProjectId();
        if (projectId == null || projectId.isBlank()) {
            projectId = firstNonBlank(
                    System.getenv("GOOGLE_CLOUD_PROJECT"),
                    System.getenv("GCLOUD_PROJECT"),
                    bucketName.substring(0, bucketName.indexOf('.') > 0
                            ? bucketName.indexOf('.') : bucketName.length()));
        }
        return StorageOptions.newBuilder()
                .setHost(host)
                .setProjectId(projectId)
                .setCredentials(EMULATOR_CREDENTIALS)
                .build()
                .getService();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
