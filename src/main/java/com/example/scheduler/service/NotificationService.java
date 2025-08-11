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

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void notify(User to, NotificationType type, String title, String message) {
        // 알림 설정 off면 무시
        if (Boolean.FALSE.equals(to.getNotificationsEnabled())) return;

        Notification n = Notification.builder()
                .user(to)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(n);
    }

    public void notifyIfFriendEnabled(User owner, User friend, NotificationType type, String title, String message) {
        if (Boolean.FALSE.equals(owner.getNotificationsEnabled())) return;
        boolean enabled = friendNotiRepo.findByOwnerAndFriend(owner, friend)
                .map(com.example.scheduler.domain.FriendNotificationSetting::isEnabled)
                .orElse(true);
        if (!enabled) return;
        notify(owner, type, title, message);
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
        // 서버 초대(INVITE)와 친구 요청(GENERIC + payload kind=friend_request)은 보존
        var all = notificationRepository.findByUserOrderByCreatedAtDesc(me);
        java.util.List<Long> deletableIds = new java.util.ArrayList<>();
        for (var n : all) {
            boolean isServerInvite = n.getType() == NotificationType.INVITE;
            boolean isFriendRequest = n.getType() == NotificationType.GENERIC
                    && n.getMessage() != null
                    && n.getMessage().contains("\"kind\":\"friend_request\"");
            if (!(isServerInvite || isFriendRequest)) {
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
}


