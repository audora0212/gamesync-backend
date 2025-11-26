package com.example.scheduler.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

public class PartyDto {

    @Data
    public static class CreateRequest {
        @NotNull(message = "서버 ID는 필수입니다")
        private Long serverId;

        @NotNull(message = "시간대는 필수입니다")
        private LocalDateTime slot;

        private Long defaultGameId;
        private Long customGameId;

        @Min(value = 1, message = "정원은 1명 이상이어야 합니다")
        @Max(value = 100, message = "정원은 100명 이하여야 합니다")
        private int capacity;
    }

    @Data
    public static class JoinRequest {
        @NotNull(message = "파티 ID는 필수입니다")
        private Long partyId;
    }

    @Data
    public static class Response {
        private Long id;
        private Long serverId;
        private String creator;
        private LocalDateTime slot;
        private Long gameId;
        private String gameName;
        private boolean custom;
        private int capacity;
        private int participants;
        private boolean full;
        private Set<String> participantNames;
        private boolean joined;
        private boolean owner;
    }
}


