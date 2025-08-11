// dto/ServerDto.java
package com.example.scheduler.dto;

import lombok.*;

import java.time.LocalTime;
import java.util.List;

public class ServerDto {

    /* ---------- 요청용 DTO ---------- */

    @Data
    public static class CreateRequest {
        private String name;
        private LocalTime resetTime;
    }

    @Data
    public static class UpdateResetTimeRequest {
        private LocalTime resetTime;
    }

    @Data                // 서버 이름 변경
    public static class UpdateNameRequest {
        private String name;
    }

    @Data
    public static class UpdateDescriptionRequest {
        private String description;
    }

    @Data
    public static class UpdateMaxMembersRequest {
        private Integer maxMembers;
    }

    @Data
    public static class ToggleResetPausedRequest {
        private boolean paused;
    }

    @Data                // 멤버 강퇴
    public static class KickRequest {
        private Long userId;
    }

    @Data                // 관리자 임명/해제
    public static class AdminRequest {
        private Long userId;
        private boolean grant;   // true: 임명, false: 해제
    }

    @Data
    public static class InviteCreateRequest {
        private Long serverId;
        private Long receiverUserId;
    }

    /* ---------- 응답용 DTO ---------- */

    @Data @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private Long ownerId;
        private String owner;
        private List<MemberInfo> members;
        private List<MemberInfo> admins;
        private LocalTime resetTime;
        private String inviteCode;
        private String description;
        private Integer maxMembers;
        private boolean resetPaused;
    }

    @Data @AllArgsConstructor
    public static class MemberInfo {
        private Long id;
        private String nickname;
    }

    @Data @AllArgsConstructor
    public static class InviteResponse {
        private Long id;
        private Long serverId;
        private String serverName;
        private Long senderId;
        private String senderNickname;
        private Long receiverId;
        private String receiverNickname;
        private String status;
        private java.time.LocalDateTime createdAt;
    }

    @Data
    public static class InviteDecisionRequest {
        private boolean accept;
    }
}
