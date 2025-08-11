package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "friend_notification_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"owner_id", "friend_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FriendNotificationSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;  // 알림을 받는 주체

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id")
    private User friend; // 친구(발신자)

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;
}


