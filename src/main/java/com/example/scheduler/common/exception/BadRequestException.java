package com.example.scheduler.common.exception;

/**
 * 잘못된 요청일 때 발생하는 예외
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BadRequestException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    // ========== Server ==========
    public static BadRequestException serverAlreadyMember() {
        return new BadRequestException(ErrorCode.SERVER_ALREADY_MEMBER);
    }

    public static BadRequestException serverOwnerCannotLeave() {
        return new BadRequestException(ErrorCode.SERVER_OWNER_CANNOT_LEAVE);
    }

    public static BadRequestException serverOwnerCannotKick() {
        return new BadRequestException(ErrorCode.SERVER_OWNER_CANNOT_KICK);
    }

    public static BadRequestException serverOwnerAlwaysAdmin() {
        return new BadRequestException(ErrorCode.SERVER_OWNER_ALWAYS_ADMIN);
    }

    public static BadRequestException serverInvalidInviteCode() {
        return new BadRequestException(ErrorCode.SERVER_INVALID_INVITE_CODE);
    }

    public static BadRequestException serverMaxMembersReached() {
        return new BadRequestException(ErrorCode.SERVER_MAX_MEMBERS_REACHED);
    }

    // ========== Invite ==========
    public static BadRequestException inviteAlreadyPending() {
        return new BadRequestException(ErrorCode.INVITE_ALREADY_PENDING);
    }

    public static BadRequestException inviteAlreadyProcessed() {
        return new BadRequestException(ErrorCode.INVITE_ALREADY_PROCESSED);
    }

    // ========== Friend ==========
    public static BadRequestException friendAlreadyExists() {
        return new BadRequestException(ErrorCode.FRIEND_ALREADY_EXISTS);
    }

    public static BadRequestException friendRequestAlreadySent() {
        return new BadRequestException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
    }

    public static BadRequestException friendCannotAddSelf() {
        return new BadRequestException(ErrorCode.FRIEND_CANNOT_ADD_SELF);
    }

    // ========== Party ==========
    public static BadRequestException partyAlreadyJoined() {
        return new BadRequestException(ErrorCode.PARTY_ALREADY_JOINED);
    }

    public static BadRequestException partyFull() {
        return new BadRequestException(ErrorCode.PARTY_FULL);
    }

    public static BadRequestException partyOwnerCannotLeave() {
        return new BadRequestException(ErrorCode.PARTY_OWNER_CANNOT_LEAVE);
    }

    // ========== Timetable ==========
    public static BadRequestException timetableInvalidTime() {
        return new BadRequestException(ErrorCode.TIMETABLE_INVALID_TIME);
    }

    // ========== General ==========
    public static BadRequestException invalidInput() {
        return new BadRequestException(ErrorCode.INVALID_INPUT_VALUE);
    }

    public static BadRequestException invalidInput(String message) {
        return new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, message);
    }
}
