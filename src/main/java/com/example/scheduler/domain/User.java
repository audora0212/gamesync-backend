// domain/User.java
package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                      // 유저 아이디

    @Column(unique = true, nullable = false)
    private String username;              // 유저 이름

    @Column(nullable = true)
    private String nickname;              // 표기될 이름

    @Column(nullable = true)
    private String password;              // 비밀번호

    @Column(unique = true)
    private String discordId;             // Discord 고유 ID

    @Column(unique = true, nullable = true)
    private String email;                 // ← 새로 추가된 이메일 필드

    @Column(length = 6, unique = true)
    private String friendCode;            // ← 친구코드(6자리 숫자)

    @ManyToMany(mappedBy = "members")
    private Set<Server> joinedServers;    // 참여중인 서버

    @Builder.Default
    @Column(nullable = true)
    private Boolean notificationsEnabled = true; // 사용자 알림 on/off (null 허용: 기존 데이터 호환)

    // 푸시 알림(FCM) 개별 카테고리 설정: null 또는 true = 켬, false = 끔
    @Builder.Default
    @Column(nullable = true)
    private Boolean pushAllEnabled = true;                // 전체 푸시 on/off

    @Builder.Default
    @Column(nullable = true)
    private Boolean pushInviteEnabled = true;             // 서버 초대 푸시

    @Builder.Default
    @Column(nullable = true)
    private Boolean pushFriendRequestEnabled = true;      // 친구 요청/응답 푸시

    @Builder.Default
    @Column(nullable = true)
    private Boolean pushFriendScheduleEnabled = true;     // 친구 스케줄 등록 푸시 (서버 내)

    // 친구 스케줄 등록 패널 표시는 pushFriendScheduleEnabled와 동일하게 간주 (필드 제거 예정)
    @Deprecated
    @Column(nullable = true)
    private Boolean panelFriendScheduleEnabled = true;

    /** 마지막 닉네임 변경 시각 (24시간 제한 용도) */
    @Column(name = "nickname_changed_at")
    private LocalDateTime nicknameChangedAt;
}
