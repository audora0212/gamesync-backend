package com.example.scheduler.repository;

import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long> {
    List<Server> findByResetTime(LocalTime resetTime);
    // 내가 속한 서버 목록 조회
    List<Server> findByMembersContains(User user);

    // 이름 검색 + 페이징
    Page<Server> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Server> findByInviteCode(String inviteCode);
}