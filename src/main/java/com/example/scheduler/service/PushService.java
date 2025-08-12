package com.example.scheduler.service;

import com.example.scheduler.domain.PushToken;
import com.example.scheduler.domain.User;
import com.example.scheduler.repository.PushTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
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
        if (firebaseApp == null || firebaseApp.isEmpty()) {
            org.slf4j.LoggerFactory.getLogger(PushService.class)
                    .warn("FirebaseApp not configured. Skipping push for userId={}", user.getId());
            return; // not configured
        }
        List<PushToken> tokens = tokenRepo.findByUser(user);
        for (PushToken pt : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(pt.getToken())
                        .putAllData(data != null ? data : java.util.Collections.emptyMap())
                        // Keep sending data-only so SW can still render; but also include body in data
                        .putData("title", title != null ? title : "GameSync")
                        .putData("body", body != null ? body : "")
                        .build();

                String messageId = FirebaseMessaging.getInstance(firebaseApp.get()).send(message);
                org.slf4j.LoggerFactory.getLogger(PushService.class)
                        .info("Sent FCM to userId={}, tokenHash={}, messageId={}",
                                user.getId(), Integer.toHexString(pt.getToken().hashCode()), messageId);
            } catch (Exception ex) {
                // InvalidRegistration, NotRegistered 등은 토큰 삭제로 정리 가능
                String msg = ex.getMessage() == null ? "" : ex.getMessage();
                if (msg.contains("registration-token-not-registered") || msg.contains("invalid-argument")) {
                    tokenRepo.deleteByToken(pt.getToken());
                    org.slf4j.LoggerFactory.getLogger(PushService.class)
                            .warn("Removed invalid FCM token tokenHash={} cause={}",
                                    Integer.toHexString(pt.getToken().hashCode()), msg);
                }
                org.slf4j.LoggerFactory.getLogger(PushService.class)
                        .error("FCM send failed for userId={}, tokenHash={}, error={}",
                                user.getId(), Integer.toHexString(pt.getToken().hashCode()), msg);
            }
        }
    }
}


