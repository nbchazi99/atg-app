package com.atg.api.web;

import com.atg.api.model.*;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/rooms")
public class RoomController extends BaseController {

    private final RoomRepository rooms;
    private final RoomMessageRepository messages;
    private final RoomPostRepository posts;
    private final RealtimeHub realtime;

    public RoomController(UserRepository users, RoomRepository rooms,
                          RoomMessageRepository messages, RoomPostRepository posts,
                          RealtimeHub realtime) {
        super(users);
        this.rooms = rooms;
        this.messages = messages;
        this.posts = posts;
        this.realtime = realtime;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        User user = me();
        return ensureCommunityRoom().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("description", r.getDescription());
            m.put("preview", r.getPreview());
            m.put("unreadCount", r.getUnreadCount());
            m.put("lastActivityAt", r.getLastActivityAt());
            m.put("locked", !MembershipAccess.canAccess(user, MembershipAccess.requiredForRoom(r)));
            m.put("requiredMembership", MembershipAccess.requiredForRoom(r));
            return m;
        }).toList();
    }

    private List<Room> ensureCommunityRoom() {
        List<Room> available = new ArrayList<>(rooms.findAll());
        boolean communityExists = available.stream()
                .anyMatch(room -> "Community".equalsIgnoreCase(room.getName()));
        if (!communityExists) {
            Room community = new Room();
            community.setName("Community");
            community.setDescription("Open chat for all ATG traders.");
            community.setPreview("");
            available.add(rooms.save(community));
        }
        available.sort(Comparator.comparing(room -> !"Community".equalsIgnoreCase(room.getName())));
        return available;
    }

    @GetMapping("/feed")
    public List<Map<String, Object>> feed(@RequestParam(defaultValue = "latest") String filter) {
        User user = me();
        List<RoomPost> rows;
        if (filter.equalsIgnoreCase("open")) rows = posts.findByOpenOrderByLatestActivityDesc(true);
        else if (filter.equalsIgnoreCase("closed")) rows = posts.findByOpenOrderByLatestActivityDesc(false);
        else rows = posts.findAllByLatestActivityDesc();

        rows = new ArrayList<>(rows);
        rows.sort(Comparator.comparing(
                (RoomPost post) -> post.getLastActivityAt() == null
                        ? post.getCreatedAt()
                        : post.getLastActivityAt()
        ).reversed());
        return rows.stream()
                .filter(p -> rooms.findById(p.getRoomId())
                        .map(r -> MembershipAccess.canAccess(user, MembershipAccess.requiredForRoom(r)))
                        .orElse(false))
                .map(this::feedItem)
                .toList();
    }

    @PostMapping("/{roomId}/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createThread(
            @PathVariable Long roomId, @Valid @RequestBody Dtos.CommunityThreadReq req) {
        User user = me();
        Room room = requireAccessibleRoom(roomId, user);
        return createThread(room, user, req);
    }

    @PostMapping("/community/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createCommunityThread(@Valid @RequestBody Dtos.CommunityThreadReq req) {
        User user = me();
        Room room = ensureCommunityRoom().stream()
                .filter(candidate -> "Community".equalsIgnoreCase(candidate.getName()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Community room could not be created"));
        MembershipAccess.requireAccess(user, MembershipAccess.requiredForRoom(room), room.getName());
        return createThread(room, user, req);
    }

    private Map<String, Object> createThread(Room room, User user, Dtos.CommunityThreadReq req) {
        Long roomId = room.getId();
        RoomPost post = new RoomPost();
        post.setRoomId(roomId);
        post.setBadge("CHAT");
        post.setTitle(req.title().trim());
        post.setPreview(req.message().trim());
        post.setParticipants(1);
        post.setCreatedById(user.getId());
        post.setCreatedByName(user.getName());
        post.setLastActivityAt(LocalDateTime.now(ZoneOffset.UTC));
        post = posts.save(post);

        saveMessage(room, user, post.getId(), req.message());
        return feedItem(post);
    }

    @PatchMapping("/{roomId}/threads/{threadId}")
    public Map<String, Object> setThreadOpen(
            @PathVariable Long roomId, @PathVariable Long threadId,
            @Valid @RequestBody Dtos.ThreadStateReq req) {
        User user = me();
        requireAccessibleRoom(roomId, user);
        RoomPost post = requireThread(roomId, threadId);
        if (!Objects.equals(post.getCreatedById(), user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the conversation creator or an administrator can change its status");
        }
        post.setOpen(req.open());
        post.setLastActivityAt(LocalDateTime.now(ZoneOffset.UTC));
        post = posts.save(post);
        realtime.publish(RealtimeHub.ROOMS, Map.of("roomId", roomId, "kind", "thread"));
        realtime.publish(RealtimeHub.roomScope(roomId), Map.of("roomId", roomId, "kind", "thread"));
        return feedItem(post);
    }

    @GetMapping("/{id}/messages")
    public List<RoomMessage> messages(@PathVariable Long id, @RequestParam(required = false) Long threadId) {
        User user = me();
        requireAccessibleRoom(id, user);
        if (threadId == null) return messages.findByRoomIdOrderByCreatedAtAsc(id);
        requireThread(id, threadId);
        return messages.findByRoomIdAndThreadIdOrderByCreatedAtAsc(id, threadId);
    }

    @PostMapping("/{id}/messages")
    public RoomMessage send(@PathVariable Long id, @RequestParam(required = false) Long threadId,
                            @Valid @RequestBody Dtos.MessageReq req) {
        User user = me();
        Room room = requireAccessibleRoom(id, user);
        if (threadId != null && !requireThread(id, threadId).isOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This conversation is closed");
        }
        String text = req.text() == null ? "" : req.text().trim();
        String attachmentUrl = req.attachmentUrl() == null ? "" : req.attachmentUrl().trim();
        if (text.isEmpty() && attachmentUrl.isEmpty()) {
            throw bad("Write a message or attach an image");
        }
        return saveMessage(room, user, threadId, text, attachmentUrl);
    }

    private Room requireAccessibleRoom(Long roomId, User user) {
        Room room = rooms.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        MembershipAccess.requireAccess(user, MembershipAccess.requiredForRoom(room), room.getName());
        return room;
    }

    private RoomPost requireThread(Long roomId, Long threadId) {
        RoomPost post = posts.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!Objects.equals(post.getRoomId(), roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
        return post;
    }

    private RoomMessage saveMessage(Room room, User user, Long threadId, String text) {
        return saveMessage(room, user, threadId, text.trim(), null);
    }

    private RoomMessage saveMessage(Room room, User user, Long threadId, String text, String attachmentUrl) {
        Long roomId = room.getId();
        RoomMessage m = new RoomMessage();
        m.setRoomId(roomId);
        m.setThreadId(threadId);
        m.setAuthorId(user.getId());
        m.setAuthor(user.getName());
        m.setAuthorAvatarUrl(user.getAvatarUrl());
        m.setText(text);
        m.setAttachmentUrl(attachmentUrl);
        m.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        messages.save(m);
        String preview = text.isEmpty() && attachmentUrl != null ? "Image" : text;
        room.setPreview(preview);
        room.setLastActivityAt(LocalDateTime.now(ZoneOffset.UTC));
        rooms.save(room);
        if (threadId != null) {
            posts.findById(threadId).ifPresent(post -> {
                post.setPreview(preview);
                post.setLastActivityAt(room.getLastActivityAt());
                posts.save(post);
            });
        }
        realtime.publish(RealtimeHub.ROOMS, Map.of("roomId", roomId, "kind", "message"));
        realtime.publish(RealtimeHub.roomScope(roomId), Map.of("roomId", roomId, "kind", "message"));
        return m;
    }

    private Map<String, Object> feedItem(RoomPost post) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", post.getId());
        m.put("roomId", post.getRoomId());
        m.put("badge", post.getBadge());
        m.put("title", post.getTitle());
        m.put("side", post.getSide());
        m.put("preview", post.getPreview());
        m.put("participants", post.getParticipants());
        m.put("open", post.isOpen());
        m.put("createdById", post.getCreatedById());
        m.put("createdByName", post.getCreatedByName());
        m.put("createdAt", post.getCreatedAt());
        m.put("lastActivityAt", post.getLastActivityAt());
        rooms.findById(post.getRoomId()).ifPresent(room -> m.put("roomName", room.getName()));
        return m;
    }

}
