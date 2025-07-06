package com.example.scheduler.dto;

import lombok.*;
import java.util.List;

public class GameDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultGameResponse {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomGameRequest {
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomGameResponse {
        private Long id;
        private String name;
    }

    @Data
    @AllArgsConstructor
    public static class DefaultGameListResponse {
        private List<DefaultGameResponse> defaultGames;
    }

    @Data
    @AllArgsConstructor
    public static class CustomGameListResponse {
        private List<CustomGameResponse> customGames;
    }

    @Data
    @AllArgsConstructor
    public static class ScheduledUserResponse {
        private String username;
    }

    @Data
    @AllArgsConstructor
    public static class ScheduledUserListResponse {
        private List<ScheduledUserResponse> users;
    }
}
