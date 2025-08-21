package com.example.scheduler.scheduler;

import com.example.scheduler.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditLogCleanupScheduler {

    private final AuditLogRepository auditRepo;

    // 매일 새벽 4시: 90일 이전 로그 삭제 (용량 보호)
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        auditRepo.deleteByOccurredAtBefore(cutoff);
    }
}


