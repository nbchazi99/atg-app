package com.atg.api.web;

import com.atg.api.model.PushToken;
import com.atg.api.model.User;
import com.atg.api.repo.PushTokenRepository;
import com.atg.api.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController extends BaseController {

    private final PushTokenRepository tokens;

    public PushController(UserRepository users, PushTokenRepository tokens) {
        super(users);
        this.tokens = tokens;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        User user = me();
        String token = body.get("token");
        String platform = body.get("platform");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }
        tokens.deleteByToken(token.trim());
        PushToken pushToken = new PushToken();
        pushToken.setUserId(user.getId());
        pushToken.setToken(token.trim());
        pushToken.setPlatform(platform == null || platform.isBlank() ? "unknown" : platform);
        tokens.save(pushToken);
        return Map.of("registered", true);
    }
}
