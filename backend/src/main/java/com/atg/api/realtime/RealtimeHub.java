package com.atg.api.realtime;

import com.atg.api.config.JwtService;
import com.atg.api.model.User;
import com.atg.api.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fan-out of lightweight invalidation events. Payloads only carry a scope and
 * non-sensitive identifiers: every client refetches through the REST API so
 * authorization stays in one place.
 */
@Component
public class RealtimeHub extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeHub.class);

    public static final String ROOMS = "rooms";
    public static final String SIGNALS = "signals";
    public static final String COURSES = "courses";
    public static final String SETTINGS = "settings";
    public static final String USERS = "users";
    public static final String REQUESTS = "requests";
    public static final String WALLET = "wallet";
    public static final String HOME = "home";

    private final JwtService jwt;
    private final UserRepository users;
    private final ObjectMapper json;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public RealtimeHub(JwtService jwt, UserRepository users, ObjectMapper json) {
        this.jwt = jwt;
        this.users = users;
        this.json = json;
    }

    public static String roomScope(Long roomId) {
        return "room:" + roomId;
    }

    public static String walletScope(Long userId) {
        return WALLET + ":" + userId;
    }

    public static String homeScope(Long userId) {
        return HOME + ":" + userId;
    }

    /** Wallet + home + admin queue invalidation for one user's request. */
    public void publishWalletChange(Long userId, String kind) {
        Map<String, Object> data = Map.of("kind", kind);
        publish(REQUESTS, data);
        if (userId == null) return;
        publish(walletScope(userId), data);
        publish(homeScope(userId), data);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = authenticate(session.getUri());
        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid or expired token"));
            return;
        }
        session.getAttributes().put("email", user.getEmail());
        session.getAttributes().put("role", user.getRole().name());
        sessions.put(session.getId(), session);
        log.info("Realtime client connected: {} ({} live)", user.getEmail(), sessions.size());
        send(session, envelope("ready", Map.of("role", user.getRole().name())));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if (payload.contains("\"ping\"")) {
            send(session, envelope("pong", Map.of()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (sessions.remove(session.getId()) != null) {
            log.info("Realtime client disconnected: {} ({} live)", session.getAttributes().get("email"), sessions.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable error) {
        sessions.remove(session.getId());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {
            // already gone
        }
    }

    public void publish(String scope) {
        publish(scope, Map.of());
    }

    public void publish(String scope, Map<String, Object> data) {
        if (sessions.isEmpty()) return;
        Map<String, Object> event = envelope("event", data);
        event.put("scope", scope);
        String payload;
        try {
            payload = json.writeValueAsString(event);
        } catch (IOException e) {
            log.warn("Could not serialise realtime event for scope {}", scope, e);
            return;
        }
        TextMessage message = new TextMessage(payload);
        sessions.values().forEach(session -> {
            if (!session.isOpen()) {
                sessions.remove(session.getId());
                return;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException | IllegalStateException e) {
                sessions.remove(session.getId());
                try {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException ignored) {
                    // already gone
                }
            }
        });
    }

    public int liveConnections() {
        return sessions.size();
    }

    private void send(WebSocketSession session, Map<String, Object> event) {
        try {
            String payload = json.writeValueAsString(event);
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException | IllegalStateException e) {
            sessions.remove(session.getId());
        }
    }

    private Map<String, Object> envelope(String type, Map<String, Object> data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("at", Instant.now().toString());
        if (!data.isEmpty()) event.putAll(data);
        return event;
    }

    private User authenticate(URI uri) {
        if (uri == null) return null;
        String token = queryParam(uri, "token");
        if (token == null || token.isBlank()) return null;
        try {
            return users.findByEmailIgnoreCase(jwt.subject(token)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String queryParam(URI uri, String name) {
        String query = uri.getRawQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            if (!part.substring(0, eq).equals(name)) continue;
            return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
        }
        return null;
    }
}
