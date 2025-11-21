package com.example.scheduler.controller;

import com.example.scheduler.dto.UserDto;
import com.example.scheduler.security.AdminGuard;
import com.example.scheduler.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AdminGuard adminGuard;

    @GetMapping
    public ResponseEntity<UserDto.Profile> getProfile(Authentication auth) {
        String username = auth.getName();
        UserDto.Profile p = userService.getProfile(username);
        boolean isAdmin = adminGuard.isAdmin(username);
        return ResponseEntity.ok()
                .header("X-Admin", isAdmin ? "true" : "false")
                .body(p);
    }

    @PutMapping("/nickname")
    public ResponseEntity<?> updateNickname(
            Authentication auth,
            @RequestBody UserDto.UpdateNickname req
    ) {
        String username = auth.getName();
        try {
            return ResponseEntity.ok(userService.updateNickname(username, req.getNickname()));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                    "message", ex.getReason() != null ? ex.getReason() : "닉네임 변경에 실패했습니다"
            ));
        }
    }

    // 내 친구코드 확인 (프론트에서 보여주면 됨)
    @GetMapping("/friend-code")
    public ResponseEntity<UserDto.FriendCode> getFriendCode(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(userService.getMyFriendCode(username));
    }

    // 사용자 알림 on/off 설정
    @PutMapping("/notifications")
    public ResponseEntity<UserDto.Profile> updateNotifications(
            Authentication auth,
            @RequestBody UserDto.UpdateNotificationSetting req
    ) {
        String username = auth.getName();
        return ResponseEntity.ok(userService.updateNotificationSetting(username, Boolean.TRUE.equals(req.getEnabled())));
    }

    // 친구별 알림 on/off 설정
    @PutMapping("/friend-notifications")
    public ResponseEntity<Void> updateFriendNotifications(
            Authentication auth,
            @RequestBody UserDto.UpdateFriendNotificationSetting req
    ) {
        String username = auth.getName();
        userService.updateFriendNotificationSetting(username, req.getFriendUserId(), Boolean.TRUE.equals(req.getEnabled()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/friend-notifications")
    public ResponseEntity<java.util.List<UserDto.FriendNotificationItem>> listFriendNotifications(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(userService.listFriendNotificationSettings(username));
    }

    // FCM 푸시 카테고리 설정 조회
    @GetMapping("/push-settings")
    public ResponseEntity<UserDto.PushSettingsResponse> getPushSettings(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(userService.getPushSettings(username));
    }

    // FCM 푸시 카테고리 설정 갱신 (null 필드는 변경 없음)
    @PutMapping("/push-settings")
    public ResponseEntity<UserDto.PushSettingsResponse> updatePushSettings(
            Authentication auth,
            @RequestBody UserDto.UpdatePushSettingsRequest req
    ) {
        String username = auth.getName();
        return ResponseEntity.ok(userService.updatePushSettings(username, req));
    }

    /** 회원 탈퇴: 서버 제외/파티/스케줄/친구/알림 등 데이터 정리 후 계정 삭제 */
    @DeleteMapping
    public ResponseEntity<Void> deleteMe(Authentication auth) {
        String username = auth.getName();
        userService.deleteCurrentUserCascade(username);
        return ResponseEntity.noContent().build();
    }
}