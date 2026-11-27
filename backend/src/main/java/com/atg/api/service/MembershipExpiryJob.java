package com.atg.api.service;

import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.PushTokenRepository;
import com.atg.api.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Downgrades VIP memberships whose paid period has lapsed. Runs daily; the
 * realtime broadcast immediately re-locks VIP rooms on every connected client.
 */
@Component
public class MembershipExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(MembershipExpiryJob.class);

    private final UserRepository users;
    private final PushTokenRepository pushTokens;
    private final PushService push;
    private final RealtimeHub realtime;

    public MembershipExpiryJob(UserRepository users, PushTokenRepository pushTokens,
                               PushService push, RealtimeHub realtime) {
        this.users = users;
        this.pushTokens = pushTokens;
        this.push = push;
        this.realtime = realtime;
    }

    @Scheduled(cron = "0 15 0 * * *", zone = "UTC")
    public void expireMemberships() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int expired = 0;
        for (User user : users.findAll()) {
            if (user.getMembership() == User.Membership.NONE) continue;
            if (user.getMembershipExpiresAt() == null) continue;
            if (!user.getMembershipExpiresAt().isBefore(now)) continue;

            String previous = user.getMembership().name();
            user.setMembership(User.Membership.NONE);
            user.setMembershipExpiresAt(null);
            users.save(user);
            expired++;

            realtime.publish(RealtimeHub.USERS,
                    Map.of("userId", user.getId(), "kind", "membership"));
            realtime.publish(RealtimeHub.homeScope(user.getId()),
                    Map.of("kind", "membership"));
            realtime.publish(RealtimeHub.ROOMS, Map.of("kind", "membership"));
            notify(user, "VIP membership ended",
                    "Your " + previous.replace('_', ' ') + " membership has expired. Renew to keep VIP access.");
            log.info("Expired {} membership for user {}", previous, user.getEmail());
        }
        if (expired > 0) log.info("Membership expiry sweep cleared {} memberships", expired);
    }

    private void notify(User user, String title, String body) {
        try {
            var tokens = pushTokens.findByUserId(user.getId()).stream()
                    .map(t -> t.getToken())
                    .filter(t -> t != null && !t.isBlank())
                    .toList();
            push.sendToTokens(tokens, title, body);
        } catch (Exception e) {
            log.warn("Could not push expiry notice to user {}", user.getId(), e);
        }
    }
}
