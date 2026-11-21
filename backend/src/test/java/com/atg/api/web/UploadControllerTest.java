package com.atg.api.web;

import com.atg.api.repo.UserRepository;
import com.atg.api.service.FirebaseCourseAssetStorage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    void rejectsCourseFilesLargerThan250Mb() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(250L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> controller.uploadCourseAsset(file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

        verifyNoInteractions(storage);
    }
}
