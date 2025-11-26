package com.example.scheduler.repository;

import com.example.scheduler.domain.Party;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {
    // 파티 목록 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"creator", "participants", "defaultGame", "customGame"})
    List<Party> findByServerOrderBySlotAsc(Server server);

    @EntityGraph(attributePaths = {"server", "creator", "participants"})
    List<Party> findByParticipantsContaining(User user);

    boolean existsByServerAndParticipantsContaining(Server server, User user);

    void deleteAllByServer(Server server);

    // 파티 상세 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"server", "creator", "participants", "defaultGame", "customGame"})
    @Query("SELECT p FROM Party p WHERE p.id = :id")
    Optional<Party> findByIdWithDetails(@Param("id") Long id);
}


