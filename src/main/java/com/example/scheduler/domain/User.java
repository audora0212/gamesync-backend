// domain/User.java
package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;
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

    @ManyToMany(mappedBy = "members")
    private Set<Server> joinedServers;    // 참여중인 서버
}
