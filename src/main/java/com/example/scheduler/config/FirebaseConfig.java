package com.example.scheduler.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson; // Base64나 plain JSON 문자열 저장 가능

    @Bean
    @ConditionalOnProperty(value = "firebase.service-account-json")
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            var app = FirebaseApp.getInstance();
            org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                    .info("Firebase already initialized: {}", app.getName());
            return app;
        }
        var credsStream = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credsStream))
                .build();
        var app = FirebaseApp.initializeApp(options);
        org.slf4j.LoggerFactory.getLogger(FirebaseConfig.class)
                .info("Firebase initialized successfully with project.");
        return app;
    }
}


