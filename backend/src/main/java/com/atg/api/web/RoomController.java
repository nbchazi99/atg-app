package com.atg.api.web;

import com.atg.api.model.*;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
            m.put("locked", !canAccess(user, r));
            m.put("requiredMembership", requiredMembership(r));
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
                .filter(p -> rooms.findById(p.getRoomId()).map(r -> canAccess(user, r)).orElse(false))
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
        requireAccess(me(), room);
        return messages.findByRoomIdOrderByCreatedAtAsc(id);
    }

    @PostMapping("/{id}/messages")
    public RoomMessage send(@PathVariable Long id, @Valid @RequestBody Dtos.MessageReq req) {
        Room room = rooms.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        User user = me();
        requireAccess(user, room);
        RoomMessage m = new RoomMessage();
        m.setRoomId(id);
        m.setAuthor(user.getName());
        m.setText(req.text().trim());
        m.setCreatedAt(LocalDateTime.now());
        messages.save(m);
        room.setPreview(req.text().trim());
        room.setLastActivityAt(LocalDateTime.now());
        rooms.save(room);
        realtime.publish(RealtimeHub.ROOMS, Map.of("roomId", id, "kind", "message"));
        realtime.publish(RealtimeHub.roomScope(id), Map.of("roomId", id, "kind", "message"));
        return m;
    }

    private static String requiredMembership(Room room) {
        if (room.getName() == null) return null;
        if (room.getName().contains("Deriv VIP")) return "DERIV_VIP";
        if (room.getName().contains("Weltrade VIP")) return "WELTRADE_VIP";
        return null;
    }

    private static boolean canAccess(User user, Room room) {
        String required = requiredMembership(room);
        if (required == null) return true;
        if (user.getRole() == User.Role.ADMIN) return true;
        return user.getMembership() != null && required.equals(user.getMembership().name());
    }

    private static void requireAccess(User user, Room room) {
        if (!canAccess(user, room)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    room.getName() + " is for members. Join from your profile to unlock it.");
        }
    }
}
