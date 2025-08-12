package com.example.scheduler.controller;

import com.example.scheduler.domain.User;
import com.example.scheduler.repository.UserRepository;
import com.example.scheduler.service.PushService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push-tokens")
@RequiredArgsConstructor
public class PushTokenController {
    private final PushService pushService;
    private final UserRepository userRepository;

    private User me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<Void> register(@RequestBody RegisterTokenRequest req) {
        pushService.registerToken(me(), req.getToken(), req.getPlatform());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unregister(@RequestParam String token) {
        pushService.unregisterToken(token);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class RegisterTokenRequest {
        private String token;
        private String platform; // "web"
    }
}


