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
    private final com.example.scheduler.repository.ServerRepository serverRepository;
    private final com.example.scheduler.repository.TimetableEntryRepository timetableEntryRepository;
    private final com.example.scheduler.repository.PartyRepository partyRepository;
    private final com.example.scheduler.repository.FriendshipRepository friendshipRepository;
    private final com.example.scheduler.repository.FriendRequestRepository friendRequestRepository;
    private final com.example.scheduler.repository.ServerInviteRepository serverInviteRepository;
    private final com.example.scheduler.repository.NotificationRepository notificationRepository;
    private final com.example.scheduler.repository.FavoriteServerRepository favoriteServerRepository;
    private final com.example.scheduler.repository.PushTokenRepository pushTokenRepository;

    public UserDto.Profile getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto.Profile(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getNotificationsEnabled(),
                user.getDiscordId() != null,
                Boolean.TRUE.equals(user.getAdmin())
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
                user.getDiscordId() != null,
                Boolean.TRUE.equals(user.getAdmin())
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
                user.getDiscordId() != null,
                Boolean.TRUE.equals(user.getAdmin())
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
                u.getPushPartyEnabled(),
                u.getPushMyTimetableReminderEnabled(),
                u.getMyTimetableReminderMinutes()
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
        if (req.getPushMyTimetableReminderEnabled() != null) u.setPushMyTimetableReminderEnabled(Boolean.TRUE.equals(req.getPushMyTimetableReminderEnabled()));
        if (req.getMyTimetableReminderMinutes() != null) u.setMyTimetableReminderMinutes(req.getMyTimetableReminderMinutes());
        userRepository.save(u);
        return new UserDto.PushSettingsResponse(
                u.getPushAllEnabled(),
                u.getPushInviteEnabled(),
                u.getPushFriendRequestEnabled(),
                u.getPushFriendScheduleEnabled(),
                // 패널 표시는 동일
                u.getPushFriendScheduleEnabled(),
                u.getPushPartyEnabled(),
                u.getPushMyTimetableReminderEnabled(),
                u.getMyTimetableReminderMinutes()
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

    /**
     * 현재 사용자 탈퇴: 모든 서버에서 제외, 해당 사용자가 만든 파티/스케줄 삭제,
     * 친구/요청/알림/즐겨찾기/푸시토큰 정리 후 최종적으로 사용자 삭제
     */
    @org.springframework.transaction.annotation.Transactional
    public void deleteCurrentUserCascade(String username) {
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 1) 내가 소유한 서버가 있으면 탈퇴 불가 (안전장치)
        java.util.List<com.example.scheduler.domain.Server> owned = serverRepository.findByOwner(me);
        if (!owned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버장을 위임하거나 서버를 삭제한 뒤 탈퇴할 수 있습니다");
        }

        // 2) 내가 멤버로 속한 모든 서버에서 제거 + 해당 서버의 내 타임테이블 삭제
        java.util.List<com.example.scheduler.domain.Server> joined = serverRepository.findByMembersContains(me);
        for (com.example.scheduler.domain.Server s : joined) {
            timetableEntryRepository.deleteAllByServerAndUser(s, me);
            s.getMembers().remove(me);
            s.getAdmins().remove(me);
            serverRepository.save(s);
        }

        // 3) 내가 만든 파티 및 내가 참가한 파티 정리
        for (com.example.scheduler.domain.Server s : joined) {
            java.util.List<com.example.scheduler.domain.Party> parties = partyRepository.findByServerOrderBySlotAsc(s);
            for (com.example.scheduler.domain.Party p : parties) {
                // 참가자에서 제거
                if (p.getParticipants().contains(me)) {
                    p.getParticipants().remove(me);
                }
                // 내가 생성한 파티는 삭제
                if (p.getCreator() != null && p.getCreator().getId().equals(me.getId())) {
                    partyRepository.delete(p);
                }
            }
        }

        // 4) 친구 관계/설정/요청/초대 제거
        var myFriendSettings = friendNotiRepo.findByOwner(me);
        friendNotiRepo.deleteAll(myFriendSettings);
        for (com.example.scheduler.domain.Friendship f : friendshipRepository.findByUser(me)) {
            friendshipRepository.delete(f);
        }
        for (com.example.scheduler.domain.Friendship f : friendshipRepository.findByFriend(me)) {
            friendshipRepository.delete(f);
        }
        // 친구 요청: 상태별로 모두 삭제
        for (com.example.scheduler.domain.FriendRequestStatus st : com.example.scheduler.domain.FriendRequestStatus.values()) {
            friendRequestRepository.findBySenderAndStatus(me, st).forEach(friendRequestRepository::delete);
            friendRequestRepository.findByReceiverAndStatus(me, st).forEach(friendRequestRepository::delete);
        }
        // 서버 초대: 발신자/수신자 모두 정리
        serverInviteRepository.findBySender(me).forEach(serverInviteRepository::delete);
        for (com.example.scheduler.domain.InviteStatus st : com.example.scheduler.domain.InviteStatus.values()) {
            try {
                serverInviteRepository.findByReceiverAndStatus(me, st).forEach(serverInviteRepository::delete);
            } catch (Exception ignored) {}
        }

        // 5) 알림/즐겨찾기/푸시토큰 삭제
        notificationRepository.deleteByUser(me);
        favoriteServerRepository.findByUser(me).forEach(fs -> favoriteServerRepository.delete(fs));
        pushTokenRepository.findByUser(me).forEach(pt -> pushTokenRepository.delete(pt));

        // 6) 최종 사용자 삭제
        userRepository.delete(me);
    }
}