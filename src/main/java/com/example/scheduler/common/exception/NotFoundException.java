package com.example.scheduler.common.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    // 편의 메서드들
    public static NotFoundException user() {
        return new NotFoundException(ErrorCode.USER_NOT_FOUND);
    }

    public static NotFoundException server() {
        return new NotFoundException(ErrorCode.SERVER_NOT_FOUND);
    }

    public static NotFoundException party() {
        return new NotFoundException(ErrorCode.PARTY_NOT_FOUND);
    }

    public static NotFoundException friend() {
        return new NotFoundException(ErrorCode.FRIEND_NOT_FOUND);
    }

    public static NotFoundException friendRequest() {
        return new NotFoundException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
    }

    public static NotFoundException invite() {
        return new NotFoundException(ErrorCode.INVITE_NOT_FOUND);
    }

    public static NotFoundException timetableEntry() {
        return new NotFoundException(ErrorCode.TIMETABLE_ENTRY_NOT_FOUND);
    }

    public static NotFoundException game() {
        return new NotFoundException(ErrorCode.GAME_NOT_FOUND);
    }

    public static NotFoundException notification() {
        return new NotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
