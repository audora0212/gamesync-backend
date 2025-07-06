package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long serverId;
    private Long userId;
    private String action;        // "REGISTER", "JOIN", "LEAVE" 등
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
    private String details;       // 추가 정보
}
