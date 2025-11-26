package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "friend_requests",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"sender_id", "receiver_id"})
        },
        indexes = {
                @Index(name = "idx_freq_sender", columnList = "sender_id"),
                @Index(name = "idx_freq_receiver", columnList = "receiver_id"),
                @Index(name = "idx_freq_status", columnList = "status"),
                @Index(name = "idx_freq_receiver_status", columnList = "receiver_id, status")
        })
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                         // 친구 요청 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;                     // 보낸 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;                   // 받는 사람

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;      // 상태: 대기/수락/거절
}


