package com.example.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

public class AuthDto {
    @Data public static class SignupRequest { //회원가입 요청
        private String username;
        private String password;
        private String nickname;
    }
    @Data @AllArgsConstructor public static class SignupResponse { //회원가입 성공여부
        private String message;
    }

    @Data public static class LoginRequest  { //로그인 요청
        private String username;
        private String password;
    }
    @Data @AllArgsConstructor public static class LoginResponse  { //로그인 성공여부, 토큰
        private String token;
        private String message;
        private Long userId;
        private String nickname;

    }
}
