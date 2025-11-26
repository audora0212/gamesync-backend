package com.example.scheduler.common.base;

import com.example.scheduler.common.exception.NotFoundException;
import com.example.scheduler.domain.User;
import com.example.scheduler.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 서비스 레이어의 기본 클래스
 * 공통 기능을 제공하여 코드 중복 제거
 */
@Slf4j
public abstract class BaseService {

    @Autowired
    protected UserRepository userRepository;

    /**
     * 현재 인증된 사용자 조회
     * SecurityContext에서 사용자 정보를 추출
     *
     * @return 현재 로그인한 User 엔티티
     * @throws NotFoundException 사용자를 찾을 수 없는 경우
     */
    protected User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw NotFoundException.user();
        }

        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(NotFoundException::user);
    }

    /**
     * 현재 사용자 ID 조회
     *
     * @return 현재 로그인한 사용자의 ID
     */
    protected Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * 현재 사용자의 username 조회
     *
     * @return 현재 로그인한 사용자의 username
     */
    protected String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    /**
     * 사용자가 인증되었는지 확인
     *
     * @return 인증 여부
     */
    protected boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal());
    }

    /**
     * ID로 사용자 조회
     *
     * @param userId 사용자 ID
     * @return User 엔티티
     * @throws NotFoundException 사용자를 찾을 수 없는 경우
     */
    protected User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(NotFoundException::user);
    }

    /**
     * 디버그 로그 출력 (개발 편의용)
     */
    protected void logDebug(String message, Object... args) {
        if (log.isDebugEnabled()) {
            log.debug(message, args);
        }
    }

    /**
     * 정보 로그 출력
     */
    protected void logInfo(String message, Object... args) {
        log.info(message, args);
    }

    /**
     * 경고 로그 출력
     */
    protected void logWarn(String message, Object... args) {
        log.warn(message, args);
    }

    /**
     * 에러 로그 출력
     */
    protected void logError(String message, Object... args) {
        log.error(message, args);
    }
}
