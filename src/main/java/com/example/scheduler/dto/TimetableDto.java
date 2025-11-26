package com.example.scheduler.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

public class TimetableDto {

    @Data
    public static class EntryRequest {
        @NotNull(message = "서버 ID는 필수입니다")
        private Long serverId;

        @NotNull(message = "시간대는 필수입니다")
        private LocalDateTime slot;

        private Long defaultGameId;
        private Long customGameId;
    }

    @Data
    public static class EntryResponse {
        private Long id;
        private String user;
        private LocalDateTime slot;
        private Long gameId;
        private String gameName;
        private boolean custom;
    }

    @Data
    @AllArgsConstructor
    public static class StatsResponse {
        private String topGame;
        private LocalDateTime avgSlot;
        private LocalDateTime peakSlot;
        private int peakCount;
    }
}
