package com.example.scheduler.repository;

import com.example.scheduler.domain.FavoriteServer;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteServerRepository extends JpaRepository<FavoriteServer, Long> {
    List<FavoriteServer> findByUser(User user);
    Optional<FavoriteServer> findByUserAndServer(User user, Server server);
    void deleteByUserAndServer(User user, Server server);
}









