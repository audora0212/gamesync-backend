package com.example.scheduler.controller;

import com.example.scheduler.domain.User;
import com.example.scheduler.dto.UserDto;
import com.example.scheduler.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserDto.Profile> getProfile(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(userService.getProfile(username));
    }

    @PutMapping("/nickname")
    public ResponseEntity<UserDto.Profile> updateNickname(
            Authentication auth,
            @RequestBody UserDto.UpdateNickname req
    ) {
        String username = auth.getName();
        return ResponseEntity.ok(userService.updateNickname(username, req.getNickname()));
    }

    // 내 친구코드 확인 (프론트에서 보여주면 됨)
    @GetMapping("/friend-code")
    public ResponseEntity<UserDto.FriendCode> getFriendCode(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(userService.getMyFriendCode(username));
    }
}