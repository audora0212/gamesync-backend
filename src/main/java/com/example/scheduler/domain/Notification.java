package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;                // 수신자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;    // 유형 (INVITE, TIMETABLE, ...)

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = true, length = 1000)
    private String message;

    @Column(nullable = false)
    private boolean read;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}


