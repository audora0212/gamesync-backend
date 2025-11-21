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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson; // Base64 또는 plain JSON 문자열

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath; // 로컬 파일 경로

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${firebase.service-account-json:}') or T(org.springframework.util.StringUtils).hasText('${firebase.service-account-path:}')")
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            var app = FirebaseApp.getInstance();
            org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                    .info("Firebase already initialized: {}", app.getName());
            return app;
        }
        String json = serviceAccountJson;
        // 1) 파일 경로가 주어졌다면 최우선 사용
        if (json == null || json.trim().isEmpty()) {
            if (serviceAccountPath != null && !serviceAccountPath.trim().isEmpty()) {
                try {
                    byte[] bytes = Files.readAllBytes(Path.of(serviceAccountPath.trim()));
                    json = new String(bytes, StandardCharsets.UTF_8);
                    org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                            .info("Loaded Firebase service account from file: {}", serviceAccountPath);
                } catch (IOException e) {
                    org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                            .error("Failed to read Firebase service account file: {}", serviceAccountPath, e);
                }
            }
        }

        // 2) 환경변수/프로퍼티로 전달된 base64 문자열 감지
        // base64로 전달된 경우 자동 디코딩 시도
        try {
            String trimmed = (json != null ? json : "").trim();
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

        if (json == null || json.trim().isEmpty()) {
            throw new IOException("Firebase service account is not configured. Provide 'firebase.service-account-path' or 'firebase.service-account-json'.");
        }

        // Try to extract project_id from the JSON and set it explicitly for FCM v1
        String projectId = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(json, Map.class);
            Object pid = map.get("project_id");
            if (pid instanceof String pidStr && !pidStr.isBlank()) {
                projectId = pidStr;
            }
        } catch (Exception ignore) {}

        var credsStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credsStream));
        if (projectId != null) {
            builder.setProjectId(projectId);
        }
        FirebaseOptions options = builder.build();
        var app = FirebaseApp.initializeApp(options);
        projectId = app.getOptions() != null ? app.getOptions().getProjectId() : projectId;
        org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                .info("Firebase initialized successfully (projectId={})", projectId);
        return app;
    }
}


