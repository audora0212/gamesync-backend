package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //커스텀 게임 아이디

    @Column(nullable = false)
    private String name; //커스텀 게임 이름

    @ManyToOne(optional = false)
    @JoinColumn(name = "server_id")
    private Server server; //어느 서버에 종속 되었는지
}
