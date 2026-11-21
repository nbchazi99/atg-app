package com.atg.api.web;

import com.atg.api.repo.UserRepository;
import com.atg.api.service.FirebaseCourseAssetStorage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UploadControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final FirebaseCourseAssetStorage storage = mock(FirebaseCourseAssetStorage.class);
    private final UploadController controller = new UploadController(users, storage);

    @Test
    void acceptsPdfAndUsesTheCanonicalContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.PDF", "image/jpeg", new byte[]{1, 2, 3});
        when(storage.store(file, ".pdf", "application/pdf"))
                .thenReturn("https://storage.example/lesson");

        assertThat(controller.uploadCourseAsset(file))
                .containsEntry("url", "https://storage.example/lesson");

        verify(storage).store(file, ".pdf", "application/pdf");
    }

    @Test
    void rejectsUnsupportedFilesBeforeStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> controller.uploadCourseAsset(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Upload an image, video, PDF, Office document, or audio file");

        verifyNoInteractions(storage);
    }
}
