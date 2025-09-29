// service/AuditService.java
package com.example.scheduler.service;

import com.example.scheduler.domain.AuditLog;
import com.example.scheduler.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepo;
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int MAX_DETAILS_LENGTH = 2000;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long serverId, Long userId, String action, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .serverId(serverId)
                    .userId(userId)
                    .action(action)
                    .details(safeDetails(details))
                    .occurredAt(LocalDateTime.now())
                    .build();
            auditLogRepo.save(entry);
        } catch (Exception e) {
            // Roll back only the audit transaction and do not affect caller
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.warn("Audit log write failed (action={}): {}", action, e.getMessage());
        }
    }

    private String safeDetails(String details) {
        if (details == null) return null;
        return (details.length() > MAX_DETAILS_LENGTH)
                ? details.substring(0, MAX_DETAILS_LENGTH)
                : details;
    }
}
