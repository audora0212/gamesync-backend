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
    private final AuditService auditService;

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

            // Body 정규화 및 클릭 URL 구성
            String pushBody = null;
            String clickUrl = null;
            Long serverIdForAudit = null;
            if (message != null) {
                String trimmed = message.trim();
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(trimmed);
                        String kind = node.has("kind") ? node.get("kind").asText(null) : null;
                        if ("friend_request".equals(kind)) {
                            // 친구 요청: 대시보드 진입 시 친구 패널 자동 오픈
                            String from = node.has("fromNickname") ? node.get("fromNickname").asText("상대방") : "상대방";
                            pushBody = from + " 님이 친구 요청을 보냈습니다.";
                            clickUrl = "/dashboard?friends=1";
                        } else if ("server_invite".equals(kind)) {
                            // 서버 초대: 초대 확인 모달 페이지로 이동 (inviteId 기반)
                            String from = node.has("fromNickname") ? node.get("fromNickname").asText("상대방") : "상대방";
                            String serverName = node.has("serverName") ? node.get("serverName").asText("") : "";
                            pushBody = from + " → " + serverName;
                            if (node.has("inviteId")) {
                                long inviteId = node.get("inviteId").asLong();
                                clickUrl = "/invite/by-id?inviteId=" + inviteId;
                            }
                        } else if ("timetable".equals(kind)) {
                            // 친구 스케줄 등록: 사람 친화적 본문 구성 + 서버 상세로 이동
                            String from = node.has("fromNickname") ? node.get("fromNickname").asText("친구") : "친구";
                            String serverName = node.has("serverName") ? node.get("serverName").asText("") : "";
                            String gameName = node.has("gameName") ? node.get("gameName").asText("") : "";
                            pushBody = String.format("%s님이 %s 서버에 %s 예약을 등록했습니다.", from, serverName, gameName);
                            if (node.has("serverId")) {
                                long serverId = node.get("serverId").asLong();
                                serverIdForAudit = serverId;
                                clickUrl = "/server/" + serverId;
                            }
                        } else if ("party".equals(kind)) {
                            // 파티 모집: 사람 친화적 본문 구성 + 서버 상세 이동(파티 탭/모달 암시 플래그)
                            String from = node.has("fromNickname") ? node.get("fromNickname").asText("사용자") : "사용자";
                            String serverName = node.has("serverName") ? node.get("serverName").asText("") : "";
                            String gameName = node.has("gameName") ? node.get("gameName").asText("") : "";
                            Integer capacity = node.has("capacity") ? node.get("capacity").asInt(0) : 0;
                            if (node.has("serverId")) {
                                long serverId = node.get("serverId").asLong();
                                serverIdForAudit = serverId;
                                clickUrl = "/server/" + serverId + "?open=party";
                            }
                            pushBody = String.format("%s님이 %s에서 %s 파티를 모집합니다 (%d명)", from, serverName, gameName, capacity);
                        } else {
                            pushBody = message;
                        }
                    } catch (Exception ignore) {
                        pushBody = message;
                    }
                } else {
                    pushBody = message;
                }
            }

            if (clickUrl != null && !clickUrl.isBlank()) {
                data.put("url", clickUrl);
            }

            boolean allow = allowPush(to, type);
            if (allow) {
                String bodyToSend = (pushBody != null && pushBody.length() <= 120) ? pushBody : null;
                pushService.pushToUser(to, title, bodyToSend, data);

                // 감사 로그: 트리거 사용자(있으면), 수신자, 타입/제목/메시지/URL 요약
                Long actorUserId = null;
                try {
                    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null) {
                        String username = auth.getName();
                        actorUserId = userRepository.findByUsername(username).map(User::getId).orElse(null);
                    }
                } catch (Exception ignored) {}

                String shortTitle = title != null ? (title.length() > 120 ? title.substring(0, 120) : title) : null;
                String shortMsg = message != null ? (message.length() > 200 ? message.substring(0, 200) : message) : null;
                String safeUrl = (clickUrl != null && clickUrl.length() > 200) ? clickUrl.substring(0, 200) : clickUrl;
                String details = String.format(
                        "toUserId=%d type=%s title=%s msg=%s url=%s",
                        to.getId(), type.name(), String.valueOf(shortTitle), String.valueOf(shortMsg), String.valueOf(safeUrl)
                );
                auditService.log(serverIdForAudit, actorUserId, "PUSH_NOTIFY", details);
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

            // 감사 로그: push-only 케이스도 동일하게 기록
            Long actorUserId = null;
            try {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    String username = auth.getName();
                    actorUserId = userRepository.findByUsername(username).map(User::getId).orElse(null);
                }
            } catch (Exception ignored) {}
            String shortTitle = title != null ? (title.length() > 120 ? title.substring(0, 120) : title) : null;
            String shortMsg = message != null ? (message.length() > 200 ? message.substring(0, 200) : message) : null;
            String details = String.format(
                    "toUserId=%d type=%s title=%s msg=%s",
                    to.getId(), type.name(), String.valueOf(shortTitle), String.valueOf(shortMsg)
            );
            auditService.log(null, actorUserId, "PUSH_NOTIFY", details);
        } catch (Exception ignored) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .warn("Push-only dispatch skipped due to exception");
        }
    }

    /** 여러 사용자에게 동일 알림을 전송하고, 하나의 감사 로그로 집계한다. */
    @Transactional
    public void notifyMany(java.util.List<User> recipients, NotificationType type, String title, String message, Long serverIdHint) {
        if (recipients == null || recipients.isEmpty()) return;
        try {
            java.util.HashMap<String, String> data = new java.util.HashMap<>();
            data.put("type", type.name());
            if (message != null) data.put("payload", message);

            String pushBody = null;
            String clickUrl = null;
            Long serverIdForAudit = serverIdHint;
            if (message != null) {
                String trimmed = message.trim();
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(trimmed);
                        String kind = node.has("kind") ? node.get("kind").asText(null) : null;
                        if ("friend_request".equals(kind)) {
                            String from = node.has("fromNickname") ? node.get("fromNickname").asText("상대방") : "상대방";
                            pushBody = from + " 님이 친구 요청을 보냈습니다.";
                            clickUrl = "/dashboard?friends=1";
                        } else if ("server_invite".equals(kind)) {
                            String from = node.has("fromNickname") ? node.get("fromNickname").asText("상대방") : "상대방";
                            String serverName = node.has("serverName") ? node.get("serverName").asText("") : "";
                            pushBody = from + " → " + serverName;
                            if (node.has("inviteId")) {
                                long inviteId = node.get("inviteId").asLong();
                                clickUrl = "/invite/by-id?inviteId=" + inviteId;
                            }
                        } else if ("timetable".equals(kind)) {
                            String from = node.has("fromNickname") ? node.get("fromNickname").asText("친구") : "친구";
                            String serverName = node.has("serverName") ? node.get("serverName").asText("") : "";
                            String gameName = node.has("gameName") ? node.get("gameName").asText("") : "";
                            pushBody = String.format("%s님이 %s 서버에 %s 예약을 등록했습니다.", from, serverName, gameName);
                            if (node.has("serverId")) {
                                long serverId = node.get("serverId").asLong();
                                serverIdForAudit = (serverIdForAudit == null) ? serverId : serverIdForAudit;
                                clickUrl = "/server/" + serverId;
                            }
                        } else if ("party".equals(kind)) {
                            String from = node.has("fromNickname") ? node.get("fromNickname").asText("사용자") : "사용자";
                            String serverName = node.has("serverName") ? node.get("serverName").asText("") : "";
                            String gameName = node.has("gameName") ? node.get("gameName").asText("") : "";
                            Integer capacity = node.has("capacity") ? node.get("capacity").asInt(0) : 0;
                            if (node.has("serverId")) {
                                long serverId = node.get("serverId").asLong();
                                serverIdForAudit = (serverIdForAudit == null) ? serverId : serverIdForAudit;
                                clickUrl = "/server/" + serverId + "?open=party";
                            }
                            pushBody = String.format("%s님이 %s에서 %s 파티를 모집합니다 (%d명)", from, serverName, gameName, capacity);
                        } else {
                            pushBody = message;
                        }
                    } catch (Exception ignore) {
                        pushBody = message;
                    }
                } else {
                    pushBody = message;
                }
            }

            if (clickUrl != null && !clickUrl.isBlank()) {
                data.put("url", clickUrl);
            }

            String bodyToSend = (pushBody != null && pushBody.length() <= 120) ? pushBody : null;
            java.util.List<User> delivered = new java.util.ArrayList<>();
            for (User r : recipients) {
                // 저장형 알림(패널) 생성: TIMETABLE은 per-user 설정에 따라 표시/미표시
                boolean skipPanel = false;
                if (type == NotificationType.TIMETABLE) {
                    Boolean on = r.getPushFriendScheduleEnabled();
                    skipPanel = Boolean.FALSE.equals(on);
                }
                if (!skipPanel) {
                    try {
                        Notification n = Notification.builder()
                                .user(r)
                                .type(type)
                                .title(title)
                                .message(message)
                                .read(false)
                                .createdAt(LocalDateTime.now())
                                .build();
                        notificationRepository.save(n);
                    } catch (Exception ignored) {}
                }

                // 푸시 전송 (카테고리별 on/off 적용)
                if (!allowPush(r, type)) continue;
                try {
                    pushService.pushToUser(r, title, bodyToSend, data);
                    delivered.add(r);
                } catch (Exception ignored) {}
            }

            // 감사 로그 집계: actor + 수신자 다수 표시
            Long actorUserId = null;
            try {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    String username = auth.getName();
                    actorUserId = userRepository.findByUsername(username).map(User::getId).orElse(null);
                }
            } catch (Exception ignored) {}

            String shortTitle = title != null ? (title.length() > 120 ? title.substring(0, 120) : title) : null;
            String shortMsg = message != null ? (message.length() > 400 ? message.substring(0, 400) : message) : null;
            String safeUrl = (clickUrl != null && clickUrl.length() > 200) ? clickUrl.substring(0, 200) : clickUrl;
            String recipientsSummary = delivered.stream()
                    .map(u -> u.getId() + "(" + (u.getNickname()!=null?u.getNickname():"") + ")")
                    .collect(java.util.stream.Collectors.joining(","));
            String details = String.format(
                    "toUsers=[%s] type=%s title=%s msg=%s url=%s",
                    recipientsSummary, type.name(), String.valueOf(shortTitle), String.valueOf(shortMsg), String.valueOf(safeUrl)
            );
            auditService.log(serverIdForAudit, actorUserId, "PUSH_NOTIFY", details);
        } catch (Exception ignored) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .warn("Push-many dispatch skipped due to exception");
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
            case PARTY -> !Boolean.FALSE.equals(to.getPushPartyEnabled());
        };
    }
}


