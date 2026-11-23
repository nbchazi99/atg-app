package com.atg.api.web;

import com.atg.api.repo.UserRepository;
import com.atg.api.repo.RoomRepository;
import com.atg.api.model.Room;
import com.atg.api.model.User;
import com.atg.api.service.FirebaseCourseAssetStorage;
import com.atg.api.service.SupportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import java.util.List;
import java.util.Optional;

class UploadControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final FirebaseCourseAssetStorage storage = mock(FirebaseCourseAssetStorage.class);
    private final SupportService support = mock(SupportService.class);
    private final UploadController controller = new UploadController(users, rooms, storage, support);
    private final User member = new User();

    @BeforeEach
    void setUp() {
        member.setId(7L);
        member.setName("Trader");
        member.setEmail("trader@example.com");
        member.setRole(User.Role.USER);
        member.setMembership(User.Membership.NONE);
        when(users.findByEmailIgnoreCase(member.getEmail())).thenReturn(Optional.of(member));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member.getEmail(), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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

    @Test
    void acceptsSupportedChatFilesAndRejectsUnsupportedTypes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "voice.OPUS", "audio/ogg", new byte[]{1, 2, 3});

        assertThat(UploadController.chatAttachmentExtension(file)).isEqualTo(".opus");

        MockMultipartFile unsupported = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", new byte[]{1});
        assertThatThrownBy(() -> UploadController.chatAttachmentExtension(unsupported))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Upload an image, video, PDF, Office document, or audio file");
    }

    @Test
    void rejectsChatFilesLargerThan80Mb() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(80L * 1024 * 1024 + 1);
        when(file.getOriginalFilename()).thenReturn("large.pdf");

        assertThatThrownBy(() -> UploadController.chatAttachmentExtension(file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void chatUploadStoresFilesPersistentlyForRoomMembers() {
        Room room = new Room();
        room.setId(3L);
        room.setName("Community");
        when(rooms.findById(3L)).thenReturn(Optional.of(room));
        MockMultipartFile file = new MockMultipartFile(
                "file", "voice.opus", "application/octet-stream", new byte[]{1, 2, 3});
        when(storage.storeChatAttachment(file, ".opus", "audio/ogg"))
                .thenReturn("https://storage.example/chat/voice.opus");

        assertThat(controller.uploadChatAttachment(3L, file))
                .containsEntry("url", "https://storage.example/chat/voice.opus");

        verify(storage).storeChatAttachment(file, ".opus", "audio/ogg");
    }

    @Test
    void chatUploadRejectsMembersWithoutVipRoomAccess() {
        Room room = new Room();
        room.setId(4L);
        room.setName("Deriv VIP");
        when(rooms.findById(4L)).thenReturn(Optional.of(room));
        MockMultipartFile file = new MockMultipartFile(
                "file", "note.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> controller.uploadChatAttachment(4L, file))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(storage, never()).storeChatAttachment(
                file, ".pdf", "application/pdf");
    }
}
