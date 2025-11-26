package com.example.scheduler.common.exception;

import com.example.scheduler.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * 모든 예외를 표준화된 ApiResponse 형식으로 반환
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.example.scheduler")
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리 - 비즈니스 로직 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {

        log.warn("Business exception: {} - {} [{}]",
                e.getErrorCode().getCode(),
                e.getMessage(),
                request.getRequestURI());

        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.error(
                        e.getErrorCode().getCode(),
                        e.getMessage()
                ));
    }

    /**
     * ResponseStatusException 처리 - 기존 코드 호환성 유지
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException e, HttpServletRequest request) {

        log.warn("Response status exception: {} [{}]",
                e.getReason(),
                request.getRequestURI());

        String code = "E" + e.getStatusCode().value();
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ApiResponse.error(code, e.getReason()));
    }

    /**
     * Validation 예외 처리 - @Valid 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {} [{}]", errors, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        "입력값 검증 실패",
                        errors
                ));
    }

    /**
     * BindException 처리 - 바인딩 실패
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException e, HttpServletRequest request) {

        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Binding failed: {} [{}]", errors, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        "입력값 바인딩 실패",
                        errors
                ));
    }

    /**
     * 타입 변환 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        log.warn("Type mismatch: {} [{}]", e.getName(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        String.format("잘못된 파라미터 타입: %s", e.getName())
                ));
    }

    /**
     * 필수 파라미터 누락 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException e, HttpServletRequest request) {

        log.warn("Missing parameter: {} [{}]", e.getParameterName(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        String.format("필수 파라미터 누락: %s", e.getParameterName())
                ));
    }

    /**
     * JSON 파싱 예외 처리
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        log.warn("Message not readable [{}]", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        "요청 본문을 읽을 수 없습니다"
                ));
    }

    /**
     * HTTP 메서드 불일치 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {

        log.warn("Method not supported: {} [{}]", e.getMethod(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(
                        ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                        String.format("지원하지 않는 HTTP 메서드: %s", e.getMethod())
                ));
    }

    /**
     * 404 Not Found 예외 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException e, HttpServletRequest request) {

        log.warn("Handler not found [{}]", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                        "요청한 리소스를 찾을 수 없습니다"
                ));
    }

    /**
     * 인증 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {

        log.warn("Authentication failed [{}]: {}", request.getRequestURI(), e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        ErrorCode.UNAUTHORIZED.getCode(),
                        "인증에 실패했습니다"
                ));
    }

    /**
     * 잘못된 인증 정보 예외 처리
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException e, HttpServletRequest request) {

        log.warn("Bad credentials [{}]", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_CREDENTIALS.getCode(),
                        "잘못된 인증 정보입니다"
                ));
    }

    /**
     * 접근 거부 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException e, HttpServletRequest request) {

        log.warn("Access denied [{}]", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        ErrorCode.ACCESS_DENIED.getCode(),
                        "접근 권한이 없습니다"
                ));
    }

    /**
     * 기타 모든 예외 처리 - 최후의 방어선
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e, HttpServletRequest request) {

        log.error("Unexpected error [{}]: ", request.getRequestURI(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        "서버 내부 오류가 발생했습니다"
                ));
    }
}
