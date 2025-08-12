package com.example.scheduler.repository;

import com.example.scheduler.domain.PushToken;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    List<PushToken> findByUser(User user);
    Optional<PushToken> findByToken(String token);
    void deleteByToken(String token);
}



