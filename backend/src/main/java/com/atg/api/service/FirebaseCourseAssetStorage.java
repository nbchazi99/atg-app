package com.atg.api.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageException;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
public class FirebaseCourseAssetStorage {

    private static final Logger log = LoggerFactory.getLogger(FirebaseCourseAssetStorage.class);
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

    private String store(MultipartFile file, String folder, String extension, String contentType) {
        if (bucketName == null || bucketName.isBlank()) {
            log.error("Firebase Storage bucket is not configured; course asset upload is unavailable");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Firebase Storage is not configured");
        }

        String objectName = folder + "/" + UUID.randomUUID() + extension;
        String downloadToken = UUID.randomUUID().toString();
        Bucket bucket;
        try {
            bucket = StorageClient.getInstance(app).bucket(bucketName);
        } catch (IllegalArgumentException | StorageException error) {
            log.error("Firebase Storage bucket {} is unavailable for course asset uploads", bucketName, error);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Firebase Storage is unavailable", error);
        }

        BlobInfo blob = BlobInfo.newBuilder(bucket.getName(), objectName)
                .setContentType(contentType)
                .setCacheControl("public,max-age=3600")
                .setMetadata(Map.of("firebaseStorageDownloadTokens", downloadToken))
                .build();
        try (InputStream input = file.getInputStream()) {
            bucket.getStorage().createFrom(blob, input);
        } catch (IOException | StorageException error) {
            log.error("Firebase Storage could not save course asset {}", objectName, error);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Could not save the course file to Firebase Storage", error);
        }
        String encodedObjectName = URLEncoder.encode(objectName, StandardCharsets.UTF_8);
        return "https://firebasestorage.googleapis.com/v0/b/" + bucket.getName()
                + "/o/" + encodedObjectName + "?alt=media&token=" + downloadToken;
    }
}
