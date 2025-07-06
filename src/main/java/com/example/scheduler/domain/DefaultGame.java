package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DefaultGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //기본 게임 아이디

    @Column(nullable = false, unique = true)
    private String name; //기본 게임 이름
}
