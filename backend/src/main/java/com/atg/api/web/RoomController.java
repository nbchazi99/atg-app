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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        return rooms.findAll().stream().map(r -> {
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

    @GetMapping("/feed")
    public List<Map<String, Object>> feed(@RequestParam(defaultValue = "all") String filter) {
        User user = me();
        List<RoomPost> rows;
        if (filter.equalsIgnoreCase("open")) rows = posts.findByOpenOrderByCreatedAtDesc(true);
        else if (filter.equalsIgnoreCase("closed")) rows = posts.findByOpenOrderByCreatedAtDesc(false);
        else rows = posts.findAllByOrderByCreatedAtDesc();

        return rows.stream()
                .filter(p -> rooms.findById(p.getRoomId())
                        .map(r -> MembershipAccess.canAccess(user, MembershipAccess.requiredForRoom(r)))
                        .orElse(false))
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("roomId", p.getRoomId());
                    m.put("badge", p.getBadge());
                    m.put("title", p.getTitle());
                    m.put("side", p.getSide());
                    m.put("preview", p.getPreview());
                    m.put("participants", p.getParticipants());
                    m.put("open", p.isOpen());
                    m.put("createdAt", p.getCreatedAt());
                    rooms.findById(p.getRoomId()).ifPresent(r -> m.put("roomName", r.getName()));
                    return m;
                }).toList();
    }

    @GetMapping("/{id}/messages")
    public List<RoomMessage> messages(@PathVariable Long id) {
        Room room = rooms.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        MembershipAccess.requireAccess(me(), MembershipAccess.requiredForRoom(room), room.getName());
        return messages.findByRoomIdOrderByCreatedAtAsc(id);
    }

    @PostMapping("/{id}/messages")
    public RoomMessage send(@PathVariable Long id, @Valid @RequestBody Dtos.MessageReq req) {
        Room room = rooms.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        User user = me();
        MembershipAccess.requireAccess(user, MembershipAccess.requiredForRoom(room), room.getName());
        RoomMessage m = new RoomMessage();
        m.setRoomId(id);
        m.setAuthor(user.getName());
        m.setText(req.text().trim());
        m.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        messages.save(m);
        room.setPreview(req.text().trim());
        room.setLastActivityAt(LocalDateTime.now(ZoneOffset.UTC));
        rooms.save(room);
        realtime.publish(RealtimeHub.ROOMS, Map.of("roomId", id, "kind", "message"));
        realtime.publish(RealtimeHub.roomScope(id), Map.of("roomId", id, "kind", "message"));
        return m;
    }

}
