package com.example.scheduler.service;

import com.example.scheduler.domain.Notification;
import com.example.scheduler.domain.NotificationType;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.NotificationDto;
import com.example.scheduler.repository.NotificationRepository;
import com.example.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final com.example.scheduler.repository.FriendNotificationSettingRepository friendNotiRepo;
    private final PushService pushService;

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void notify(User to, NotificationType type, String title, String message) {
        // 알림 설정 off면 무시
        if (Boolean.FALSE.equals(to.getNotificationsEnabled())) return;

        // 친구 스케줄 등록의 경우, panelFriendScheduleEnabled=true일 때만 저장형 알림 생성
        boolean skipPanel = false;
        if (type == NotificationType.TIMETABLE) {
            // 이제 친구 스케줄 등록 패널 표시는 pushFriendScheduleEnabled 값과 동일하게 동작
            Boolean on = to.getPushFriendScheduleEnabled();
            // null 또는 true => 표시(ON)
            skipPanel = Boolean.FALSE.equals(on);
        }
        Notification n = null;
        if (!skipPanel) {
            n = Notification.builder()
                    .user(to)
                    .type(type)
                    .title(title)
                    .message(message)
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(n);
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .info("Saved notification id={} type={} toUserId={}", n.getId(), type, to.getId());
        }

        // 푸시 전송 (best-effort) - 카테고리별 on/off 적용
        try {
            java.util.HashMap<String, String> data = new java.util.HashMap<>();
            data.put("type", type.name());
            if (message != null) data.put("payload", message);
            boolean allow = allowPush(to, type);
            if (allow) {
                pushService.pushToUser(to, title, (message != null && message.length() <= 120) ? message : null, data);
            }
        } catch (Exception ignored) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .warn("Push dispatch skipped due to exception");
        }
    }

    /** 저장형 알림을 생성하지 않고 FCM만 전송한다 (벨 패널 비표시). */
    public void notifyPushOnly(User to, NotificationType type, String title, String message) {
        try {
            if (!allowPush(to, type)) return;
            java.util.HashMap<String, String> data = new java.util.HashMap<>();
            data.put("type", type.name());
            if (message != null) data.put("payload", message);
            pushService.pushToUser(to, title, (message != null && message.length() <= 120) ? message : null, data);
        } catch (Exception ignored) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .warn("Push-only dispatch skipped due to exception");
        }
    }

    public void notifyIfFriendEnabled(User owner, User friend, NotificationType type, String title, String message) {
        if (Boolean.FALSE.equals(owner.getNotificationsEnabled())) return;
        boolean enabled = friendNotiRepo.findByOwnerAndFriend(owner, friend)
                .map(com.example.scheduler.domain.FriendNotificationSetting::isEnabled)
                .orElse(true);
        if (!enabled) return;
        notify(owner, type, title, message);
    }

    /** 친구별 on/off와 전체 설정을 준수하면서, 저장형 알림 없이 FCM만 전송 */
    public void notifyPushOnlyIfFriendEnabled(User owner, User friend, NotificationType type, String title, String message) {
        if (Boolean.FALSE.equals(owner.getNotificationsEnabled())) return;
        boolean enabled = friendNotiRepo.findByOwnerAndFriend(owner, friend)
                .map(com.example.scheduler.domain.FriendNotificationSetting::isEnabled)
                .orElse(true);
        if (!enabled) return;
        notifyPushOnly(owner, type, title, message);
    }

    @Transactional(readOnly = true)
    public NotificationDto.NotificationListResponse listMine() {
        User me = currentUser();
        List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(me);
        long unread = notificationRepository.countByUserAndReadIsFalse(me);
        return new NotificationDto.NotificationListResponse(
                list.stream().map(this::toDto).collect(Collectors.toList()),
                unread
        );
    }

    @Transactional
    public void markAsRead(Long id) {
        User me = currentUser();
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!n.getUser().getId().equals(me.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void clearAllMine() {
        User me = currentUser();
        // 서버 초대 중 실제 초대(payload에 kind=server_invite 포함)와 친구 요청만 보존
        var all = notificationRepository.findByUserOrderByCreatedAtDesc(me);
        java.util.List<Long> deletableIds = new java.util.ArrayList<>();
        for (var n : all) {
            boolean isActionableServerInvite = n.getType() == NotificationType.INVITE
                    && n.getMessage() != null
                    && n.getMessage().contains("\"kind\":\"server_invite\"");
            boolean isFriendRequest = n.getType() == NotificationType.GENERIC
                    && n.getMessage() != null
                    && n.getMessage().contains("\"kind\":\"friend_request\"");
            // 수락 알림 등 payload 없는 INVITE 메시지는 삭제 대상으로 간주
            if (!(isActionableServerInvite || isFriendRequest)) {
                deletableIds.add(n.getId());
            }
        }
        if (!deletableIds.isEmpty()) {
            notificationRepository.deleteAllByIdInBatch(deletableIds);
        }
    }

    private NotificationDto.NotificationResponse toDto(Notification n) {
        return new NotificationDto.NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    /** 현재 사용자 알림 중, 타입과 메시지 일부 일치로 삭제 */
    @Transactional
    public void deleteMineByMessageFragment(NotificationType type, String messageFragment) {
        User me = currentUser();
        notificationRepository.deleteByUserAndTypeAndMessageContaining(me, type, messageFragment);
    }

    private boolean allowPush(User to, NotificationType type) {
        // 전체 스위치가 false면 모든 FCM 차단
        if (Boolean.FALSE.equals(to.getPushAllEnabled())) return false;
        // 타입별 스위치
        return switch (type) {
            case INVITE -> !Boolean.FALSE.equals(to.getPushInviteEnabled());
            case GENERIC -> !Boolean.FALSE.equals(to.getPushFriendRequestEnabled());
            case TIMETABLE -> !Boolean.FALSE.equals(to.getPushFriendScheduleEnabled());
        };
    }
}


