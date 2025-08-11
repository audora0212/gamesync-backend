package com.example.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

public class UserDto {
    @Data @AllArgsConstructor
    public static class Profile {
        private Long id;
        private String username;
        private String nickname;
        private String email;
    }

    @Data
    public static class UpdateNickname {
        private String nickname;
    }

    @Data @AllArgsConstructor
    public static class FriendCode {
        private String code;            // 6자리 코드
    }
}