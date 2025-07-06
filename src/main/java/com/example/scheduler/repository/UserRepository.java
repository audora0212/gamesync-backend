// repository/UserRepository.java
package com.example.scheduler.repository;

import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    // OAuth용 Discord ID 조회 추가
    Optional<User> findByDiscordId(String discordId);
}