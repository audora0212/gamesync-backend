package com.example.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDto {
    @Data @AllArgsConstructor
    public static class NotificationResponse {
        private Long id;
        private String type;
        private String title;
        private String message;
        private boolean read;
        private LocalDateTime createdAt;
    }

    @Data @AllArgsConstructor
    public static class NotificationListResponse {
        private List<NotificationResponse> notifications;
        private long unreadCount;
    }
}


