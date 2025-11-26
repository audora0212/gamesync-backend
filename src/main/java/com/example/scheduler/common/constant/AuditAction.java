package com.example.scheduler.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 감사 로그 액션 상수 정의
 * 하드코딩된 문자열 대신 타입 안전한 상수 사용
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditAction {

    // ========== Server ==========
    public static final String CREATE_SERVER = "CREATE_SERVER";
    public static final String DELETE_SERVER = "DELETE_SERVER";
    public static final String UPDATE_SERVER = "UPDATE_SERVER";
    public static final String JOIN_SERVER = "JOIN_SERVER";
    public static final String LEAVE_SERVER = "LEAVE_SERVER";
    public static final String KICK_MEMBER = "KICK_MEMBER";
    public static final String CHANGE_ADMIN = "CHANGE_ADMIN";
    public static final String GRANT_ADMIN = "GRANT_ADMIN";
    public static final String REVOKE_ADMIN = "REVOKE_ADMIN";

    // ========== Party ==========
    public static final String CREATE_PARTY = "CREATE_PARTY";
    public static final String DELETE_PARTY = "DELETE_PARTY";
    public static final String JOIN_PARTY = "JOIN_PARTY";
    public static final String LEAVE_PARTY = "LEAVE_PARTY";
    public static final String KICK_PARTY_MEMBER = "KICK_PARTY_MEMBER";

    // ========== Timetable ==========
    public static final String CREATE_TIMETABLE = "CREATE_TIMETABLE";
    public static final String UPDATE_TIMETABLE = "UPDATE_TIMETABLE";
    public static final String DELETE_TIMETABLE = "DELETE_TIMETABLE";
    public static final String RESET_TIMETABLE = "RESET_TIMETABLE";

    // ========== User ==========
    public static final String USER_LOGIN = "USER_LOGIN";
    public static final String USER_LOGOUT = "USER_LOGOUT";
    public static final String USER_REGISTER = "USER_REGISTER";
    public static final String UPDATE_PROFILE = "UPDATE_PROFILE";
    public static final String DELETE_ACCOUNT = "DELETE_ACCOUNT";

    // ========== Friend ==========
    public static final String SEND_FRIEND_REQUEST = "SEND_FRIEND_REQUEST";
    public static final String ACCEPT_FRIEND_REQUEST = "ACCEPT_FRIEND_REQUEST";
    public static final String REJECT_FRIEND_REQUEST = "REJECT_FRIEND_REQUEST";
    public static final String DELETE_FRIEND = "DELETE_FRIEND";

    // ========== Invite ==========
    public static final String CREATE_INVITE = "CREATE_INVITE";
    public static final String ACCEPT_INVITE = "ACCEPT_INVITE";
    public static final String REJECT_INVITE = "REJECT_INVITE";

    // ========== Game ==========
    public static final String CREATE_GAME = "CREATE_GAME";
    public static final String DELETE_GAME = "DELETE_GAME";

    // ========== Admin ==========
    public static final String ADMIN_BAN_USER = "ADMIN_BAN_USER";
    public static final String ADMIN_UNBAN_USER = "ADMIN_UNBAN_USER";
    public static final String ADMIN_DELETE_SERVER = "ADMIN_DELETE_SERVER";
}
