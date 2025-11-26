package com.example.scheduler.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

public class ServerDto {

    /* ---------- 요청용 DTO ---------- */

    @Data
    public static class CreateRequest {
        @NotBlank(message = "서버 이름은 필수입니다")
        @Size(min = 1, max = 50, message = "서버 이름은 1~50자여야 합니다")
        private String name;

        @NotNull(message = "리셋 시간은 필수입니다")
        private LocalTime resetTime;
    }

    @Data
    public static class UpdateResetTimeRequest {
        @NotNull(message = "리셋 시간은 필수입니다")
        private LocalTime resetTime;
    }

    @Data
    public static class UpdateNameRequest {
        @NotBlank(message = "서버 이름은 필수입니다")
        @Size(min = 1, max = 50, message = "서버 이름은 1~50자여야 합니다")
        private String name;
    }

    @Data
    public static class UpdateDescriptionRequest {
        @Size(max = 500, message = "설명은 500자 이내여야 합니다")
        private String description;
    }

    @Data
    public static class UpdateMaxMembersRequest {
        @Min(value = 1, message = "최대 멤버 수는 1 이상이어야 합니다")
        @Max(value = 1000, message = "최대 멤버 수는 1000 이하여야 합니다")
        private Integer maxMembers;
    }

    @Data
    public static class ToggleResetPausedRequest {
        private boolean paused;
    }

    @Data
    public static class KickRequest {
        @NotNull(message = "대상 사용자 ID는 필수입니다")
        private Long userId;
    }

    @Data
    public static class AdminRequest {
        @NotNull(message = "대상 사용자 ID는 필수입니다")
        private Long userId;
        private boolean grant;
    }

    @Data
    public static class InviteCreateRequest {
        @NotNull(message = "서버 ID는 필수입니다")
        private Long serverId;

        @NotNull(message = "수신자 ID는 필수입니다")
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
