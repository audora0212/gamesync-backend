// repository/AuditLogRepository.java
package com.example.scheduler.repository;

import com.example.scheduler.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByServerIdAndActionAndOccurredAtBetween(Long serverId, String action, LocalDateTime start, LocalDateTime end);
    AuditLog findFirstByServerIdAndActionOrderByOccurredAtAsc(Long serverId, String action);
    long deleteByOccurredAtBefore(LocalDateTime cutoff);

    // 페이지네이션 지원
    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);

    Page<AuditLog> findByServerIdOrderByOccurredAtDesc(Long serverId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.action IN :actions ORDER BY a.occurredAt DESC")
    Page<AuditLog> findByActionInOrderByOccurredAtDesc(@Param("actions") Collection<String> actions, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.serverId = :serverId AND a.action IN :actions ORDER BY a.occurredAt DESC")
    Page<AuditLog> findByServerIdAndActionInOrderByOccurredAtDesc(
            @Param("serverId") Long serverId,
            @Param("actions") Collection<String> actions,
            Pageable pageable);

    Page<AuditLog> findByServerIdAndActionOrderByOccurredAtDesc(Long serverId, String action, Pageable pageable);
}
