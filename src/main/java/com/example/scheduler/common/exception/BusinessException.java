package com.example.scheduler.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 예외의 기본 클래스
 * 모든 도메인별 예외는 이 클래스를 상속받아 구현
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
    }
}
