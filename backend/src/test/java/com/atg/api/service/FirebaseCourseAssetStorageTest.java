package com.atg.api.service;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FirebaseCourseAssetStorageTest {

    @Test
    void constructionDoesNotRequireTheStorageBucketToExist() {
        assertThatCode(() -> new FirebaseCourseAssetStorage(
                mock(FirebaseApp.class), "missing-bucket"))
                .doesNotThrowAnyException();
    }

    @Test
    void missingBucketConfigurationReturnsServiceUnavailableForUploads() {
        FirebaseCourseAssetStorage storage = new FirebaseCourseAssetStorage(
                mock(FirebaseApp.class), " ");
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> storage.store(file, ".pdf", "application/pdf"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
