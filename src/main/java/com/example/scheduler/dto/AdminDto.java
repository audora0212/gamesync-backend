package com.example.scheduler.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class AdminDto {
    @Data @AllArgsConstructor
    public static class AuditLogItem {
        private Long id;
        private Long serverId;
        private Long userId;
        private String userNickname;
        private String action;
        private String details;
        private LocalDateTime occurredAt;
    }

    @Data @AllArgsConstructor
    public static class ServerItem {
        private Long id;
        private String name;
        private Long ownerId;
        private String ownerNickname;
        private LocalTime resetTime;
        private int members;
    }

    @Data @AllArgsConstructor
    public static class TimetableItem {
        private Long id;
        private Long serverId;
        private String serverName;
        private Long userId;
        private String userNickname;
        private LocalDateTime slot;
        private String gameName;
    }

    @Data @AllArgsConstructor
    public static class PartyItem {
        private Long id;
        private Long serverId;
        private String serverName;
        private Long creatorId;
        private String creatorNickname;
        private LocalDateTime slot;
        private int capacity;
        private String gameName;
        private int participants;
    }

    // ========== Admin Upsert Request DTOs (JSON Injection 방지) ==========

    @Data
    public static class ServerUpsertRequest {
        private Long id;  // null이면 생성, 값이 있으면 수정

        @NotBlank(message = "서버 이름은 필수입니다")
        @Size(max = 50, message = "서버 이름은 50자 이하여야 합니다")
        private String name;

        @NotNull(message = "리셋 시간은 필수입니다")
        private LocalTime resetTime;

        @Size(max = 500, message = "설명은 500자 이하여야 합니다")
        private String description;

        private Integer maxMembers;
        private boolean resetPaused;
    }

    @Data
    public static class TimetableUpsertRequest {
        private Long id;  // null이면 생성, 값이 있으면 수정

        @NotNull(message = "서버 ID는 필수입니다")
        private Long serverId;

        @NotNull(message = "사용자 ID는 필수입니다")
        private Long userId;

        @NotNull(message = "시간대는 필수입니다")
        private LocalDateTime slot;

        private Long defaultGameId;
        private Long customGameId;
    }

    @Data
    public static class PartyUpsertRequest {
        private Long id;  // null이면 생성, 값이 있으면 수정

        @NotNull(message = "서버 ID는 필수입니다")
        private Long serverId;

        @NotNull(message = "생성자 ID는 필수입니다")
        private Long creatorId;

        @NotNull(message = "시간대는 필수입니다")
        private LocalDateTime slot;

        @Min(value = 1, message = "정원은 1명 이상이어야 합니다")
        private int capacity;

        private Long defaultGameId;
        private Long customGameId;
    }
}


