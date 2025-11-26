package com.example.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

public class NoticeDto {

    @Data
    public static class Upsert {
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다")
        private String title;

        @NotBlank(message = "내용은 필수입니다")
        @Size(max = 10000, message = "내용은 10000자 이하여야 합니다")
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


