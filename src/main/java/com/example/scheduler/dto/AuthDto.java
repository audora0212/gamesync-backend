package com.example.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

public class AuthDto {

    @Data
    public static class SignupRequest {
        @NotBlank(message = "아이디는 필수입니다")
        @Size(min = 3, max = 50, message = "아이디는 3~50자여야 합니다")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "아이디는 영문, 숫자, 언더스코어만 허용됩니다")
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 128, message = "비밀번호는 8~128자여야 합니다")
        private String password;

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 1, max = 30, message = "닉네임은 1~30자여야 합니다")
        private String nickname;
    }

    @Data
    @AllArgsConstructor
    public static class SignupResponse {
        private String message;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "아이디는 필수입니다")
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String message;
        private Long userId;
        private String nickname;
    }
}
