package com.example.scheduler.scheduler;

import com.example.scheduler.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class BlacklistCleanupScheduler {

    private final BlacklistedTokenRepository blacklistRepo;

    /* 매일 새벽 3시 정각: 만료된 블랙리스트 토큰 제거 */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        blacklistRepo.deleteAllByExpiryBefore(new Date());
    }
}
