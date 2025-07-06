// domain/Server.java
package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.Set;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                      // 서버 ID

    @Column(nullable = false)
    private String name;                  // 서버 이름

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;                   // 서버장

    @ManyToMany
    @JoinTable(name = "server_members",
            joinColumns = @JoinColumn(name = "server_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members;            // 서버 멤버

    @Column(nullable = false, unique = true, length = 6)
    private String inviteCode;

    @ManyToMany
    @JoinTable(name = "server_admins",
            joinColumns = @JoinColumn(name = "server_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> admins;

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TimetableEntry> entries;  // 타임테이블 엔트리

    @Column(nullable = false)
    private LocalTime resetTime;          // 타임테이블 초기화 시각
}
