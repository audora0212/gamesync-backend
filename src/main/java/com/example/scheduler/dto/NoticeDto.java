package com.example.scheduler.dto;

import lombok.*;
import java.time.LocalDateTime;

public class NoticeDto {

    @Data
    public static class Upsert {
        private String title;
        private String content;
    }

    @Data
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String title;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @AllArgsConstructor
    public static class Detail {
        private Long id;
        private String title;
        private String content;
        private String author;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}


