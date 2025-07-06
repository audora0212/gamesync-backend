package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;          // 실제 JWT 문자열

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiry;           // JWT 만료 시각
}
