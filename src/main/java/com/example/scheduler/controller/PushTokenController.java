package com.example.scheduler.controller;

import com.example.scheduler.domain.User;
import com.example.scheduler.repository.UserRepository;
import com.example.scheduler.service.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for registering and unregistering FCM push tokens.
 */
@RestController
@RequestMapping("/api/push-tokens")
@RequiredArgsConstructor
public class PushTokenController {

    private final PushService pushService;
    private final UserRepository userRepository;

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    }

    public record RegisterRequest(String token, String platform) {}

    @PostMapping
    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
        if (req == null || req.token() == null || req.token().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        pushService.registerToken(currentUser(), req.token(), req.platform());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unregister(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        pushService.unregisterToken(token);
        return ResponseEntity.noContent().build();
    }
}


