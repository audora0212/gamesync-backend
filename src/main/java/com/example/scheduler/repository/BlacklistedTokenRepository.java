package com.example.scheduler.repository;

import com.example.scheduler.domain.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);
    void deleteAllByExpiryBefore(Date now);      // 만료된 블랙리스트 토큰 정리
}
