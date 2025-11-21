package com.example.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

public class FriendDto {

    @Data
    public static class SendRequest {
        private Long receiverId;        // 누구에게 보낼지
        private String friendCode;      // 혹은 친구코드로 보낼지
    }

    @Data
    public static class RespondRequest {
        private boolean accept;         // 수락(true) / 거절(false)
    }

    @Data @AllArgsConstructor
    public static class SimpleUser {
        private Long id;
        private String username;
        private String nickname;
    }

    @Data @AllArgsConstructor
    public static class FriendListResponse {
        private List<SimpleUser> friends;   // 친구 목록
    }

    @Data @AllArgsConstructor
    public static class PendingRequest {
        private Long requestId;             // 친구 요청 ID
        private SimpleUser user;            // 상대 유저 (보낸/받은에 따라 의미 달라짐)
    }

    @Data @AllArgsConstructor
    public static class PendingListResponse {
        private List<PendingRequest> requests;  // 대기중 요청(보낸/받은)
    }
}


