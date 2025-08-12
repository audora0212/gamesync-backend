package com.example.scheduler.service;

import com.example.scheduler.domain.PushToken;
import com.example.scheduler.domain.User;
import com.example.scheduler.repository.PushTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PushService {
    private final PushTokenRepository tokenRepo;
    // FirebaseApp may be null if not configured; treat as optional
    private final java.util.Optional<com.google.firebase.FirebaseApp> firebaseApp;

    @Transactional
    public void registerToken(User user, String token, String platform) {
        PushToken pt = tokenRepo.findByToken(token).orElse(
                PushToken.builder()
                        .user(user)
                        .token(token)
                        .platform(platform == null ? "web" : platform)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );
        pt.setUser(user);
        pt.setPlatform(platform == null ? "web" : platform);
        pt.setUpdatedAt(LocalDateTime.now());
        tokenRepo.save(pt);
        org.slf4j.LoggerFactory.getLogger(PushService.class)
                .info("Registered FCM token for userId={}, platform={}, tokenHash={}",
                        user.getId(), pt.getPlatform(), Integer.toHexString(token.hashCode()));
    }

    @Transactional
    public void unregisterToken(String token) {
        tokenRepo.deleteByToken(token);
        org.slf4j.LoggerFactory.getLogger(PushService.class)
                .info("Unregistered FCM token tokenHash={}", Integer.toHexString(token.hashCode()));
    }

    /**
     * Fire-and-forget push; failures for invalid tokens should prune tokens eventually
     */
    public void pushToUser(User user, String title, String body, java.util.Map<String, String> data) {
        var logger = org.slf4j.LoggerFactory.getLogger(PushService.class);
        if (firebaseApp == null || firebaseApp.isEmpty()) {
            logger.warn("FirebaseApp not configured. Skipping push for userId={}", user.getId());
            return; // not configured
        }
        List<PushToken> tokens = tokenRepo.findByUser(user);
        if (tokens == null || tokens.isEmpty()) {
            logger.info("No FCM tokens for userId={}. Skipping.", user.getId());
            return;
        }

        logger.info("Dispatching FCM to userId={}, tokenCount={}, title='{}'", user.getId(), tokens.size(), title);
        for (PushToken pt : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(pt.getToken())
                        .putAllData(data != null ? data : java.util.Collections.emptyMap())
                        .putData("title", title != null ? title : "GameSync")
                        .putData("body", body != null ? body : "")
                        .build();

                String messageId = FirebaseMessaging.getInstance(firebaseApp.get()).send(message);
                logger.info("FCM sent ok userId={}, platform={}, tokenHash={}, messageId={}",
                        user.getId(), pt.getPlatform(), Integer.toHexString(pt.getToken().hashCode()), messageId);
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? "" : ex.getMessage();
                if (ex instanceof FirebaseMessagingException fme) {
                    logger.error("FCM failed userId={}, platform={}, tokenHash={}, code={}, msg={}",
                            user.getId(), pt.getPlatform(), Integer.toHexString(pt.getToken().hashCode()),
                            String.valueOf(fme.getErrorCode()), fme.getMessage());
                } else {
                    logger.error("FCM failed userId={}, platform={}, tokenHash={}, msg={}",
                            user.getId(), pt.getPlatform(), Integer.toHexString(pt.getToken().hashCode()), msg);
                }

                // InvalidRegistration, NotRegistered 등 토큰 정리
                if (msg.contains("registration-token-not-registered") || msg.contains("invalid-argument") || msg.contains("UNREGISTERED")) {
                    tokenRepo.deleteByToken(pt.getToken());
                    logger.warn("Removed invalid FCM token tokenHash={}", Integer.toHexString(pt.getToken().hashCode()));
                }
            }
        }
    }
}


