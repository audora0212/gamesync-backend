package com.example.scheduler.repository;

import com.example.scheduler.domain.CustomGame;
import com.example.scheduler.domain.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomGameRepository extends JpaRepository<CustomGame, Long> {
    List<CustomGame> findByServer(Server server);
}
