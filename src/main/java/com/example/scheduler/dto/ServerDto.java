// dto/ServerDto.java
package com.example.scheduler.dto;

import lombok.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public class ServerDto {

    /* ---------- 요청용 DTO ---------- */

    @Data
    public static class CreateRequest {
        private String name;
        private LocalTime resetTime;
    }

    @Data
    public static class UpdateResetTimeRequest {
        private LocalTime resetTime;
    }

    @Data                // 서버 이름 변경
    public static class UpdateNameRequest {
        private String name;
    }

    @Data                // 멤버 강퇴
    public static class KickRequest {
        private Long userId;
    }

    @Data                // 관리자 임명/해제
    public static class AdminRequest {
        private Long userId;
        private boolean grant;   // true: 임명, false: 해제
    }

    /* ---------- 응답용 DTO ---------- */

    @Data @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private Long ownerId;
        private String owner;
        private List<MemberInfo> members;
        private List<MemberInfo> admins;
        private LocalTime resetTime;
        private String inviteCode;
    }

    @Data @AllArgsConstructor
    public static class MemberInfo {
        private Long id;
        private String nickname;
    }
}
