package com.example.scheduler.service;

import com.example.scheduler.domain.User;
import com.example.scheduler.dto.UserDto;
import com.example.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
        user.setNickname(newNickname);
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

    /** 현재 로그인 사용자의 디스코드 연동 해제 */
    public void unlinkDiscordCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setDiscordId(null);
        userRepository.save(user);
    }
}