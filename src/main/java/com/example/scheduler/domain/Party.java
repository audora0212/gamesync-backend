package com.example.scheduler.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "server_id")
    private Server server;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(nullable = false)
    private LocalDateTime slot;

    @ManyToOne
    @JoinColumn(name = "default_game_id")
    private DefaultGame defaultGame;

    @ManyToOne
    @JoinColumn(name = "custom_game_id")
    private CustomGame customGame;

    @Column(nullable = false)
    private int capacity;

    @ManyToMany
    @JoinTable(name = "party_participants",
            joinColumns = @JoinColumn(name = "party_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<User> participants = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;
}




