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
        private Boolean notificationsEnabled;
        private Boolean discordLinked;
    }

    @Data
    public static class UpdateNickname {
        private String nickname;
    }

    @Data @AllArgsConstructor
    public static class FriendCode {
        private String code;            // 6자리 코드
    }

    @Data
    public static class UpdateNotificationSetting {
        private Boolean enabled;
    }

    @Data
    public static class UpdateFriendNotificationSetting {
        private Long friendUserId;
        private Boolean enabled;
    }

    @Data @AllArgsConstructor
    public static class FriendNotificationItem {
        private Long friendUserId;
        private String friendNickname;
        private Boolean enabled;
    }

    // 푸시 설정 응답
    @Data @AllArgsConstructor
    public static class PushSettingsResponse {
        private Boolean pushAllEnabled;
        private Boolean pushInviteEnabled;
        private Boolean pushFriendRequestEnabled;
        private Boolean pushFriendScheduleEnabled;
        private Boolean panelFriendScheduleEnabled;
        private Boolean pushPartyEnabled;
        private Boolean pushMyTimetableReminderEnabled;
        private Integer myTimetableReminderMinutes;
    }

    // 푸시 설정 갱신 요청 (null이면 변경 없음)
    @Data
    public static class UpdatePushSettingsRequest {
        private Boolean pushAllEnabled;
        private Boolean pushInviteEnabled;
        private Boolean pushFriendRequestEnabled;
        private Boolean pushFriendScheduleEnabled;
        private Boolean panelFriendScheduleEnabled;
        private Boolean pushPartyEnabled;
        private Boolean pushMyTimetableReminderEnabled;
        private Integer myTimetableReminderMinutes;
    }
}