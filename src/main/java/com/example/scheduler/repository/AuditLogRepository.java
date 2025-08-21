// repository/AuditLogRepository.java
package com.example.scheduler.repository;

import com.example.scheduler.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByServerIdAndActionAndOccurredAtBetween(Long serverId, String action, LocalDateTime start, LocalDateTime end);
    AuditLog findFirstByServerIdAndActionOrderByOccurredAtAsc(Long serverId, String action);
    long deleteByOccurredAtBefore(LocalDateTime cutoff);
}
