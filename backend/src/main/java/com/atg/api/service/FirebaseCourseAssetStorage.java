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
    private final Bucket bucket;

    public FirebaseCourseAssetStorage(
            FirebaseApp app,
            @Value("${app.firebase.storage-bucket:}") String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("Firebase Storage bucket is not configured");
        }
        this.bucket = StorageClient.getInstance(app).bucket(bucketName);
    }

    public String store(MultipartFile file, String extension, String contentType) {
        String objectName = "course-assets/" + UUID.randomUUID() + extension;
        String downloadToken = UUID.randomUUID().toString();
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
