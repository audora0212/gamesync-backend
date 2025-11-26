package com.example.scheduler.common.exception;

/**
 * 권한이 없을 때 발생하는 예외
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    // 편의 메서드들
    public static ForbiddenException serverNotMember() {
        return new ForbiddenException(ErrorCode.SERVER_NOT_MEMBER);
    }

    public static ForbiddenException serverAdminRequired() {
        return new ForbiddenException(ErrorCode.SERVER_ADMIN_REQUIRED);
    }

    public static ForbiddenException serverOwnerRequired() {
        return new ForbiddenException(ErrorCode.SERVER_OWNER_REQUIRED);
    }

    public static ForbiddenException partyNotMember() {
        return new ForbiddenException(ErrorCode.PARTY_NOT_MEMBER);
    }

    public static ForbiddenException inviteOnlyFriends() {
        return new ForbiddenException(ErrorCode.INVITE_ONLY_FRIENDS);
    }

    public static ForbiddenException inviteOnlyMember() {
        return new ForbiddenException(ErrorCode.INVITE_ONLY_MEMBER);
    }

    public static ForbiddenException inviteNotReceiver() {
        return new ForbiddenException(ErrorCode.INVITE_NOT_RECEIVER);
    }

    public static ForbiddenException timetableNotOwner() {
        return new ForbiddenException(ErrorCode.TIMETABLE_NOT_OWNER);
    }

    public static ForbiddenException notificationNotOwner() {
        return new ForbiddenException(ErrorCode.NOTIFICATION_NOT_OWNER);
    }

    public static ForbiddenException accessDenied() {
        return new ForbiddenException(ErrorCode.ACCESS_DENIED);
    }
}
