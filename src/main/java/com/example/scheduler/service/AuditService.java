// service/AuditService.java
package com.example.scheduler.service;

import com.example.scheduler.domain.AuditLog;
import com.example.scheduler.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepo;


    public void log(Long serverId, Long userId, String action, String details) {
        AuditLog entry = AuditLog.builder()
                .serverId(serverId)
                .userId(userId)
                .action(action)
                .details(details)
                .occurredAt(LocalDateTime.now())
                .build();
        auditLogRepo.save(entry);
    }
}
