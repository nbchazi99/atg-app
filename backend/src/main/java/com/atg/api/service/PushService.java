package com.atg.api.service;

import com.atg.api.repo.PushTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final PushTokenRepository tokens;
    private final FirebaseMessaging messaging;

    public PushService(FirebaseApp firebaseApp, PushTokenRepository tokens) {
        this.tokens = tokens;
        this.messaging = FirebaseMessaging.getInstance(firebaseApp);
    }

    public boolean isEnabled() {
        return messaging != null;
    }

    public int sendToTokens(List<String> tokenList, String title, String body) {
        if (!isEnabled() || tokenList.isEmpty()) return 0;
        int sent = 0;
        for (String token : tokenList) {
            try {
                messaging.send(Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build());
                sent++;
            } catch (FirebaseMessagingException e) {
                if (MessagingErrorCode.INVALID_ARGUMENT.equals(e.getMessagingErrorCode())
                        || MessagingErrorCode.UNREGISTERED.equals(e.getMessagingErrorCode())) {
                    tokens.deleteByToken(token);
                }
                log.warn("Push send failed for a token: {} ({})", e.getMessage(), e.getMessagingErrorCode());
            } catch (Exception e) {
                log.warn("Push send failed for a token", e);
            }
        }
        return sent;
    }
}
