package com.example.scheduler.repository;

import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long> {
    List<Server> findByResetTime(LocalTime resetTime);

    // 내가 속한 서버 목록 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"owner", "members", "admins"})
    List<Server> findByMembersContains(User user);

    // 내가 소유한 서버 목록 조회
    @EntityGraph(attributePaths = {"owner", "members", "admins"})
    List<Server> findByOwner(User owner);

    // 이름 검색 + 페이징
    @EntityGraph(attributePaths = {"owner"})
    Page<Server> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "members", "admins"})
    Optional<Server> findByInviteCode(String inviteCode);

    Optional<Server> findByDiscordGuildId(String discordGuildId);

    // 서버 상세 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"owner", "members", "admins"})
    @Query("SELECT s FROM Server s WHERE s.id = :id")
    Optional<Server> findByIdWithMembers(@Param("id") Long id);
}