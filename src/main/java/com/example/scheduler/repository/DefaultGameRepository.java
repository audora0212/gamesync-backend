package com.example.scheduler.repository;

import com.example.scheduler.domain.DefaultGame;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DefaultGameRepository extends JpaRepository<DefaultGame, Long> {
    Optional<DefaultGame> findByName(String name);
}
