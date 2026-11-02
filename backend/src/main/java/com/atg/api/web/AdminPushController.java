package com.atg.api.web;

import com.atg.api.repo.PushTokenRepository;
import com.atg.api.repo.UserRepository;
import com.atg.api.service.PushService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/push")
public class AdminPushController extends BaseController {

    private final PushTokenRepository tokens;
    private final PushService push;

    public AdminPushController(UserRepository users, PushTokenRepository tokens, PushService push) {
        super(users);
        this.tokens = tokens;
        this.push = push;
    }

    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody Map<String, String> body) {
        if (!push.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Push is not configured. Set app.firebase.service-account.");
        }
        String title = body.get("title");
        String message = body.get("body");
        String userIdRaw = body.get("userId");
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }
        var tokenList = (userIdRaw == null || userIdRaw.isBlank()
                ? tokens.findAll()
                : tokens.findByUserId(Long.parseLong(userIdRaw)))
                .stream().map(t -> t.getToken()).toList();
        if (tokenList.isEmpty()) {
            return Map.of("sent", 0, "tokens", 0);
        }
        int sent = push.sendToTokens(tokenList, title, message == null ? "" : message);
        return Map.of("sent", sent, "tokens", tokenList.size());
    }
}
