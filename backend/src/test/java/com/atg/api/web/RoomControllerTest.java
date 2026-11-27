package com.atg.api.web;

import com.atg.api.model.Room;
import com.atg.api.model.RoomMessage;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.RoomMessageRepository;
import com.atg.api.repo.RoomPostRepository;
import com.atg.api.repo.RoomRepository;
import com.atg.api.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomControllerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomMessageRepository messages = mock(RoomMessageRepository.class);
    private final RoomPostRepository posts = mock(RoomPostRepository.class);
    private final RealtimeHub realtime = mock(RealtimeHub.class);

    private final User member = new User();
    private RoomController controller;

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
        controller = new RoomController(users, rooms, messages, posts, realtime);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAndListsAnUnlockedCommunityRoomForAnOrdinaryMember() {
        when(rooms.findAll()).thenReturn(new ArrayList<>());
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(1L);
            return room;
        });

        List<Map<String, Object>> result = controller.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0))
                .containsEntry("name", "Community")
                .containsEntry("locked", false)
                .containsEntry("requiredMembership", null);
    }

    @Test
    void listsCommunityBeforeVipRooms() {
        Room vip = new Room();
        vip.setId(2L);
        vip.setName("Deriv VIP");
        Room community = new Room();
        community.setId(3L);
        community.setName("Community");
        when(rooms.findAll()).thenReturn(List.of(vip, community));

        List<Map<String, Object>> result = controller.list();

        assertThat(result).hasSize(2);
        assertThat(result.get(0))
                .containsEntry("name", "Community")
                .containsEntry("locked", false);
        assertThat(result.get(1))
                .containsEntry("name", "Deriv VIP")
                .containsEntry("locked", true);
    }

    @Test
    void hidesVipRoomActivityFromMembersWithoutTheMatchingMembership() {
        Room vip = new Room();
        vip.setId(2L);
        vip.setName("Deriv VIP");
        vip.setPreview("Private Deriv setup");
        vip.setLastActivityAt(LocalDateTime.now());
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        when(rooms.findAll()).thenReturn(List.of(vip, community));

        Map<String, Object> listedVip = controller.list().stream()
                .filter(room -> "Deriv VIP".equals(room.get("name")))
                .findFirst()
                .orElseThrow();

        assertThat(listedVip)
                .containsEntry("locked", true)
                .containsEntry("preview", "")
                .containsEntry("lastActivityAt", null);
    }

    @Test
    void ordinaryMemberCanSendMessagesToCommunity() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        when(rooms.findById(1L)).thenReturn(Optional.of(community));
        when(messages.save(any(RoomMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomMessage sent = controller.send(1L, null, new Dtos.MessageReq("  Hello traders  ", null));

        assertThat(sent.getAuthorId()).isEqualTo(member.getId());
        assertThat(sent.getText()).isEqualTo("Hello traders");
        verify(messages).save(sent);
        verify(realtime).publish(RealtimeHub.ROOMS, Map.of("roomId", 1L, "kind", "message"));
    }

    @Test
    void ordinaryMemberCanSendAnAttachmentWithoutText() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        when(rooms.findById(1L)).thenReturn(Optional.of(community));
        when(messages.save(any(RoomMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomMessage sent = controller.send(
                1L, null, new Dtos.MessageReq(
                        "", "https://atg.example/uploads/chart.png", "IMAGE"));

        assertThat(sent.getText()).isEmpty();
        assertThat(sent.getAttachmentUrl()).isEqualTo("https://atg.example/uploads/chart.png");
        assertThat(sent.getAttachmentType()).isEqualTo("IMAGE");
        assertThat(community.getPreview()).isEqualTo("Attachment");
        verify(messages).save(sent);
    }

    @Test
    void ordinaryMemberCanStartCommunityConversationWithItsFirstMessage() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        when(rooms.findById(1L)).thenReturn(Optional.of(community));
        when(posts.save(any())).thenAnswer(invocation -> {
            com.atg.api.model.RoomPost post = invocation.getArgument(0);
            post.setId(42L);
            return post;
        });
        when(messages.save(any(RoomMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> created = controller.createThread(
                1L, new Dtos.CommunityThreadReq("  Trading ideas  ", "  What are you watching today?  "));

        assertThat(created)
                .containsEntry("id", 42L)
                .containsEntry("roomId", 1L)
                .containsEntry("title", "Trading ideas")
                .containsEntry("preview", "What are you watching today?")
                .containsEntry("createdById", member.getId())
                .containsEntry("createdByName", member.getName())
                .containsEntry("open", true);
        org.mockito.ArgumentCaptor<RoomMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(RoomMessage.class);
        verify(messages).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getRoomId()).isEqualTo(1L);
        assertThat(messageCaptor.getValue().getThreadId()).isEqualTo(42L);
        assertThat(messageCaptor.getValue().getText()).isEqualTo("What are you watching today?");
    }

    @Test
    void createsMissingCommunityRoomWhenStartingConversation() {
        when(rooms.findAll()).thenReturn(new ArrayList<>());
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(1L);
            return room;
        });
        when(posts.save(any())).thenAnswer(invocation -> {
            com.atg.api.model.RoomPost post = invocation.getArgument(0);
            post.setId(42L);
            return post;
        });
        when(messages.save(any(RoomMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> created = controller.createCommunityThread(
                new Dtos.CommunityThreadReq("Topic", "First message"));

        assertThat(created)
                .containsEntry("roomId", 1L)
                .containsEntry("title", "Topic")
                .containsEntry("preview", "First message");
        verify(rooms, org.mockito.Mockito.times(2)).save(any(Room.class));
        verify(messages).save(any(RoomMessage.class));
    }

    @Test
    void latestFeedIncludesBothOpenAndClosedCommunityConversations() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        com.atg.api.model.RoomPost open = new com.atg.api.model.RoomPost();
        open.setId(11L);
        open.setRoomId(1L);
        open.setTitle("Open");
        open.setLastActivityAt(LocalDateTime.now().minusHours(1));
        com.atg.api.model.RoomPost closed = new com.atg.api.model.RoomPost();
        closed.setId(12L);
        closed.setRoomId(1L);
        closed.setTitle("Closed");
        closed.setOpen(false);
        closed.setLastActivityAt(LocalDateTime.now());
        when(posts.findAllByLatestActivityDesc()).thenReturn(List.of(open, closed));
        when(rooms.findById(1L)).thenReturn(Optional.of(community));

        List<Map<String, Object>> result = controller.feed("latest");

        assertThat(result).extracting(item -> item.get("title")).containsExactly("Closed", "Open");
        assertThat(result).extracting(item -> item.get("open")).containsExactly(false, true);
    }

    @Test
    void creatorCanCloseAndReopenConversationButOtherMembersCannot() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        com.atg.api.model.RoomPost post = new com.atg.api.model.RoomPost();
        post.setId(42L);
        post.setRoomId(1L);
        post.setCreatedById(member.getId());
        when(rooms.findById(1L)).thenReturn(Optional.of(community));
        when(posts.findById(42L)).thenReturn(Optional.of(post));
        when(posts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        controller.setThreadOpen(1L, 42L, new Dtos.ThreadStateReq(false));
        assertThat(post.isOpen()).isFalse();
        controller.setThreadOpen(1L, 42L, new Dtos.ThreadStateReq(true));
        assertThat(post.isOpen()).isTrue();

        User other = new User();
        other.setId(8L);
        other.setName("Other");
        other.setEmail("other@example.com");
        other.setRole(User.Role.USER);
        when(users.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(other.getEmail(), null, List.of()));

        assertThatThrownBy(() -> controller.setThreadOpen(1L, 42L, new Dtos.ThreadStateReq(false)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void closedConversationRejectsNewMessages() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        com.atg.api.model.RoomPost post = new com.atg.api.model.RoomPost();
        post.setId(42L);
        post.setRoomId(1L);
        post.setOpen(false);
        when(rooms.findById(1L)).thenReturn(Optional.of(community));
        when(posts.findById(42L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> controller.send(1L, 42L, new Dtos.MessageReq("Still here", null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void ordinaryMemberCannotStartConversationInVipRoom() {
        Room vip = new Room();
        vip.setId(2L);
        vip.setName("Deriv VIP");
        when(rooms.findById(2L)).thenReturn(Optional.of(vip));

        assertThatThrownBy(() -> controller.createThread(
                2L, new Dtos.CommunityThreadReq("Topic", "Message")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void loadsMessagesForOnlyTheRequestedConversation() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        com.atg.api.model.RoomPost post = new com.atg.api.model.RoomPost();
        post.setId(42L);
        post.setRoomId(1L);
        RoomMessage message = new RoomMessage();
        message.setId(5L);
        message.setRoomId(1L);
        message.setThreadId(42L);
        message.setText("Thread reply");
        when(rooms.findById(1L)).thenReturn(Optional.of(community));
        when(posts.findById(42L)).thenReturn(Optional.of(post));
        when(messages.findByRoomIdAndThreadIdOrderByCreatedAtAsc(1L, 42L))
                .thenReturn(List.of(message));

        assertThat(controller.messages(1L, 42L)).containsExactly(message);
        verify(messages).findByRoomIdAndThreadIdOrderByCreatedAtAsc(1L, 42L);
    }

    @Test
    void showsTheAuthorsCurrentUsernameAndAvatarForExistingMessages() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        member.setUsername("trader");
        member.setAvatarUrl("https://storage.example/profile-avatars/7/avatar.jpg");
        RoomMessage message = new RoomMessage();
        message.setId(5L);
        message.setRoomId(1L);
        message.setAuthorId(member.getId());
        message.setAuthor("Old name");
        message.setAuthorAvatarUrl("https://storage.example/old-avatar.jpg");
        when(rooms.findById(1L)).thenReturn(Optional.of(community));
        when(messages.findByRoomIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(message));
        when(users.findById(member.getId())).thenReturn(Optional.of(member));

        List<RoomMessage> result = controller.messages(1L, null);

        assertThat(result).containsExactly(message);
        assertThat(message.getAuthor()).isEqualTo("trader");
        assertThat(message.getAuthorAvatarUrl())
                .isEqualTo("https://storage.example/profile-avatars/7/avatar.jpg");
    }

    @Test
    void ordinaryAndVipMembersShareCommunityMessages() {
        Room community = new Room();
        community.setId(1L);
        community.setName("Community");
        List<RoomMessage> sharedMessages = new ArrayList<>();
        when(rooms.findById(1L)).thenReturn(Optional.of(community));
        when(messages.findByRoomIdOrderByCreatedAtAsc(1L)).thenAnswer(
                ignored -> new ArrayList<>(sharedMessages));
        when(messages.save(any(RoomMessage.class))).thenAnswer(invocation -> {
            RoomMessage message = invocation.getArgument(0);
            sharedMessages.add(message);
            return message;
        });
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomMessage ordinaryMessage = controller.send(
                1L, null, new Dtos.MessageReq("Hello from a regular member", null));
        member.setMembership(User.Membership.DERIV_VIP);
        assertThat(controller.messages(1L, null)).containsExactly(ordinaryMessage);

        RoomMessage vipMessage = controller.send(1L, null, new Dtos.MessageReq("Hello from VIP", null));
        assertThat(vipMessage.getText()).isEqualTo("Hello from VIP");
        member.setMembership(User.Membership.NONE);
        assertThat(controller.messages(1L, null)).containsExactly(ordinaryMessage, vipMessage);
        verify(messages).save(ordinaryMessage);
        verify(messages).save(vipMessage);
    }

    @Test
    void ordinaryMemberCannotPostToVipRoomsWithoutMembership() {
        Room vip = new Room();
        vip.setId(2L);
        vip.setName("Deriv VIP");
        when(rooms.findById(2L)).thenReturn(Optional.of(vip));

        assertThatThrownBy(() -> controller.send(2L, null, new Dtos.MessageReq("Hello", null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
