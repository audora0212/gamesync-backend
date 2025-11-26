// domain/Server.java
package com.example.scheduler.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "servers", indexes = {
        @Index(name = "idx_server_reset_time", columnList = "resetTime"),
        @Index(name = "idx_server_invite_code", columnList = "inviteCode"),
        @Index(name = "idx_server_owner", columnList = "owner_id"),
        @Index(name = "idx_server_discord_guild", columnList = "discordGuildId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                      // 서버 ID

    @NotBlank(message = "서버 이름은 필수입니다")
    @Size(min = 1, max = 50, message = "서버 이름은 1~50자여야 합니다")
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

    @NotBlank(message = "초대 코드는 필수입니다")
    @Size(min = 6, max = 6, message = "초대 코드는 6자리여야 합니다")
    @Column(nullable = false, unique = true, length = 6)
    private String inviteCode;

    @ManyToMany
    @JoinTable(name = "server_admins",
            joinColumns = @JoinColumn(name = "server_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> admins;

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TimetableEntry> entries;  // 타임테이블 엔트리

    @NotNull(message = "리셋 시간은 필수입니다")
    @Column(nullable = false)
    private LocalTime resetTime;          // 타임테이블 초기화 시각

    @Column
    private String description;           // 서버 설명

    @Column
    private Integer maxMembers;           // 최대 멤버 수 (null이면 무제한)

    @Builder.Default
    @Column(nullable = false)
    private boolean resetPaused = false;  // 리셋 일시중지

    @Column(unique = true)
    private String discordGuildId;        // 연결된 디스코드 서버 ID
}
