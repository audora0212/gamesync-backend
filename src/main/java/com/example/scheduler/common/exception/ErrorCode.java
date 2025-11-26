package com.example.scheduler.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전체 에러 코드 정의
 * 도메인별로 그룹화하여 관리
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========== Common (C) ==========
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력값입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "허용되지 않은 메서드입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C006", "인증이 필요합니다"),

    // ========== User (U) ==========
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 존재하는 사용자입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "U003", "잘못된 인증 정보입니다"),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "U004", "이미 사용 중인 닉네임입니다"),

    // ========== Server (S) ==========
    SERVER_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "서버를 찾을 수 없습니다"),
    SERVER_ALREADY_MEMBER(HttpStatus.BAD_REQUEST, "S002", "이미 참가한 서버입니다"),
    SERVER_NOT_MEMBER(HttpStatus.FORBIDDEN, "S003", "서버 멤버가 아닙니다"),
    SERVER_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "S004", "관리자 권한이 필요합니다"),
    SERVER_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "S005", "서버장 권한이 필요합니다"),
    SERVER_OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "S006", "서버장은 떠날 수 없습니다"),
    SERVER_OWNER_CANNOT_KICK(HttpStatus.BAD_REQUEST, "S007", "서버장은 강퇴할 수 없습니다"),
    SERVER_OWNER_ALWAYS_ADMIN(HttpStatus.BAD_REQUEST, "S008", "서버장은 항상 관리자입니다"),
    SERVER_INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "S009", "유효하지 않은 초대 코드입니다"),
    SERVER_MAX_MEMBERS_REACHED(HttpStatus.BAD_REQUEST, "S010", "서버 최대 인원에 도달했습니다"),

    // ========== Invite (I) ==========
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "초대를 찾을 수 없습니다"),
    INVITE_ALREADY_PENDING(HttpStatus.BAD_REQUEST, "I002", "이미 대기 중인 초대가 있습니다"),
    INVITE_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "I003", "이미 처리된 초대입니다"),
    INVITE_NOT_RECEIVER(HttpStatus.FORBIDDEN, "I004", "내 초대만 응답할 수 있습니다"),
    INVITE_ONLY_FRIENDS(HttpStatus.FORBIDDEN, "I005", "친구만 초대할 수 있습니다"),
    INVITE_ONLY_MEMBER(HttpStatus.FORBIDDEN, "I006", "서버 멤버만 초대할 수 있습니다"),

    // ========== Friend (F) ==========
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "친구를 찾을 수 없습니다"),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "F002", "친구 요청을 찾을 수 없습니다"),
    FRIEND_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "F003", "이미 친구입니다"),
    FRIEND_REQUEST_ALREADY_SENT(HttpStatus.BAD_REQUEST, "F004", "이미 친구 요청을 보냈습니다"),
    FRIEND_CANNOT_ADD_SELF(HttpStatus.BAD_REQUEST, "F005", "자기 자신을 친구로 추가할 수 없습니다"),
    FRIEND_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "F006", "친구 코드를 찾을 수 없습니다"),

    // ========== Party (P) ==========
    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "파티를 찾을 수 없습니다"),
    PARTY_ALREADY_JOINED(HttpStatus.BAD_REQUEST, "P002", "이미 참가한 파티입니다"),
    PARTY_FULL(HttpStatus.BAD_REQUEST, "P003", "파티가 가득 찼습니다"),
    PARTY_OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "P004", "파티장은 떠날 수 없습니다"),
    PARTY_NOT_MEMBER(HttpStatus.FORBIDDEN, "P005", "파티 멤버가 아닙니다"),

    // ========== Timetable (T) ==========
    TIMETABLE_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "타임테이블 항목을 찾을 수 없습니다"),
    TIMETABLE_NOT_OWNER(HttpStatus.FORBIDDEN, "T002", "본인의 타임테이블만 수정할 수 있습니다"),
    TIMETABLE_INVALID_TIME(HttpStatus.BAD_REQUEST, "T003", "유효하지 않은 시간입니다"),

    // ========== Game (G) ==========
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "G001", "게임을 찾을 수 없습니다"),
    GAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "G002", "이미 존재하는 게임입니다"),

    // ========== Notification (N) ==========
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다"),
    NOTIFICATION_NOT_OWNER(HttpStatus.FORBIDDEN, "N002", "본인의 알림만 조작할 수 있습니다"),

    // ========== Auth (A) ==========
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A001", "토큰이 만료되었습니다"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다"),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "A003", "블랙리스트에 등록된 토큰입니다"),
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_REQUEST, "A004", "OAuth 인증 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
