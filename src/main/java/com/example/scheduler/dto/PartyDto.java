package com.example.scheduler.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

public class PartyDto {
    @Data
    public static class CreateRequest {
        private Long serverId;
        private LocalDateTime slot;
        private Long defaultGameId;
        private Long customGameId;
        private int capacity;
    }

    @Data
    public static class JoinRequest {
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
    }
}


