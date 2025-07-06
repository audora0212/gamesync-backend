package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimetableEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //테이블 기록 아이디

    @ManyToOne(optional = false)
    @JoinColumn(name = "server_id")
    private Server server; //테이블 기록할 서버 아이디

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user; //테이블 기록할 유저 아이디

    @Column(nullable = false)
    private LocalDateTime slot; //어느 시간 슬롯에 넣을지

    @ManyToOne
    @JoinColumn(name = "default_game_id")
    private DefaultGame defaultGame; //일반 게임인 경우

    @ManyToOne
    @JoinColumn(name = "custom_game_id")
    private CustomGame customGame; //커스텀 게임인 경우
}
