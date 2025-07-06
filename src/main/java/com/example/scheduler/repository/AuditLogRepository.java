// repository/AuditLogRepository.java
package com.example.scheduler.repository;

import com.example.scheduler.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
