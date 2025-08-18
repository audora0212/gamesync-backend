package com.example.scheduler.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson; // Base64 또는 plain JSON 문자열

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${firebase.service-account-json:}')")
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            var app = FirebaseApp.getInstance();
            org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                    .info("Firebase already initialized: {}", app.getName());
            return app;
        }
        String json = serviceAccountJson;
        // base64로 전달된 경우 자동 디코딩 시도
        try {
            String trimmed = serviceAccountJson.trim();
            // 간단 휴리스틱: '{'로 시작하지 않으면 base64로 간주
            if (!trimmed.startsWith("{")) {
                byte[] decoded = java.util.Base64.getDecoder().decode(trimmed);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                if (decodedStr.trim().startsWith("{")) {
                    json = decodedStr;
                    org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                            .info("Loaded Firebase service account from base64 env var");
                }
            }
        } catch (IllegalArgumentException ignore) {
            // base64 디코딩 실패 시 그대로 사용
        }

        var credsStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credsStream))
                .build();
        var app = FirebaseApp.initializeApp(options);
        String projectId = app.getOptions() != null ? app.getOptions().getProjectId() : null;
        org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                .info("Firebase initialized successfully (projectId={})", projectId);
        return app;
    }
}


