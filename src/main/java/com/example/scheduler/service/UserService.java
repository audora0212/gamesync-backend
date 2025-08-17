package com.example.scheduler.service;

import com.example.scheduler.domain.User;
import com.example.scheduler.dto.UserDto;
import com.example.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final com.example.scheduler.repository.FriendNotificationSettingRepository friendNotiRepo;

    public UserDto.Profile getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto.Profile(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getNotificationsEnabled(),
                user.getDiscordId() != null
        );
    }

    public UserDto.Profile updateNickname(String username, String newNickname) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // 24시간 제한 체크
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastChangedAt = user.getNicknameChangedAt();
        if (lastChangedAt != null) {
            LocalDateTime availableAt = lastChangedAt.plusHours(24);
            if (now.isBefore(availableAt)) {
                Duration remaining = Duration.between(now, availableAt);
                long totalMinutes = Math.max(0, remaining.toMinutes());
                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;
                String message = String.format(
                        "닉네임 변경은 24시간에 한 번만 가능합니다. %d시간 %d분 뒤에 닉네임 변경이 가능합니다.",
                        hours, minutes
                );
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
            }
        }
        user.setNickname(newNickname);
        user.setNicknameChangedAt(now);
        userRepository.save(user);
        return new UserDto.Profile(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getNotificationsEnabled(),
                user.getDiscordId() != null
        );
    }

    // 내 친구코드 확인
    public UserDto.FriendCode getMyFriendCode(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto.FriendCode(user.getFriendCode());
    }

    public UserDto.Profile updateNotificationSetting(String username, boolean enabled) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setNotificationsEnabled(enabled);
        userRepository.save(user);
        return new UserDto.Profile(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getNotificationsEnabled(),
                user.getDiscordId() != null
        );
    }

    public void updateFriendNotificationSetting(String username, Long friendUserId, boolean enabled) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User friend = userRepository.findById(friendUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var setting = friendNotiRepo.findByOwnerAndFriend(owner, friend)
                .orElseGet(() -> com.example.scheduler.domain.FriendNotificationSetting.builder()
                        .owner(owner)
                        .friend(friend)
                        .enabled(true)
                        .build());
        setting.setEnabled(enabled);
        friendNotiRepo.save(setting);
    }

    public java.util.List<UserDto.FriendNotificationItem> listFriendNotificationSettings(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var list = friendNotiRepo.findByOwner(owner);
        return list.stream().map(s -> new UserDto.FriendNotificationItem(
                s.getFriend().getId(),
                s.getFriend().getNickname(),
                s.isEnabled()
        )).toList();
    }

    public UserDto.PushSettingsResponse getPushSettings(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto.PushSettingsResponse(
                u.getPushAllEnabled(),
                u.getPushInviteEnabled(),
                u.getPushFriendRequestEnabled(),
                u.getPushFriendScheduleEnabled(),
                // 패널 표시는 pushFriendScheduleEnabled와 동일 취급
                u.getPushFriendScheduleEnabled(),
                u.getPushPartyEnabled()
        );
    }

    public UserDto.PushSettingsResponse updatePushSettings(String username, UserDto.UpdatePushSettingsRequest req) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // 전체 스위치 우선 처리: 켬/끔 시 하위도 동일하게 설정
        if (req.getPushAllEnabled() != null) {
            boolean on = Boolean.TRUE.equals(req.getPushAllEnabled());
            u.setPushAllEnabled(on);
            u.setPushInviteEnabled(on);
            u.setPushFriendRequestEnabled(on);
            u.setPushFriendScheduleEnabled(on);
            u.setPushPartyEnabled(on);
        }
        if (req.getPushInviteEnabled() != null) u.setPushInviteEnabled(Boolean.TRUE.equals(req.getPushInviteEnabled()));
        if (req.getPushFriendRequestEnabled() != null) u.setPushFriendRequestEnabled(Boolean.TRUE.equals(req.getPushFriendRequestEnabled()));
        if (req.getPushFriendScheduleEnabled() != null) u.setPushFriendScheduleEnabled(Boolean.TRUE.equals(req.getPushFriendScheduleEnabled()));
        if (req.getPanelFriendScheduleEnabled() != null) u.setPanelFriendScheduleEnabled(Boolean.TRUE.equals(req.getPanelFriendScheduleEnabled()));
        if (req.getPushPartyEnabled() != null) u.setPushPartyEnabled(Boolean.TRUE.equals(req.getPushPartyEnabled()));
        userRepository.save(u);
        return new UserDto.PushSettingsResponse(
                u.getPushAllEnabled(),
                u.getPushInviteEnabled(),
                u.getPushFriendRequestEnabled(),
                u.getPushFriendScheduleEnabled(),
                // 패널 표시는 동일
                u.getPushFriendScheduleEnabled(),
                u.getPushPartyEnabled()
        );
    }

    /** 현재 로그인 사용자의 디스코드 연동 해제 */
    public void unlinkDiscordCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setDiscordId(null);
        userRepository.save(user);
    }
}