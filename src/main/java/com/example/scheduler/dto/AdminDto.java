package com.example.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

public class AdminDto {
    @Data @AllArgsConstructor
    public static class AuditLogItem {
        private Long id;
        private Long serverId;
        private Long userId;
        private String action;
        private String details;
        private LocalDateTime occurredAt;
    }

    @Data @AllArgsConstructor
    public static class ServerItem {
        private Long id;
        private String name;
        private Long ownerId;
        private java.time.LocalTime resetTime;
        private int members;
    }

    @Data @AllArgsConstructor
    public static class TimetableItem {
        private Long id;
        private Long serverId;
        private Long userId;
        private LocalDateTime slot;
        private String gameName;
    }

    @Data @AllArgsConstructor
    public static class PartyItem {
        private Long id;
        private Long serverId;
        private Long creatorId;
        private LocalDateTime slot;
        private int capacity;
        private String gameName;
        private int participants;
    }
}


