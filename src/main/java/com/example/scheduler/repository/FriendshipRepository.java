package com.example.scheduler.repository;

import com.example.scheduler.domain.Friendship;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    // 친구 목록 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"user", "friend"})
    List<Friendship> findByUser(User user);

    @EntityGraph(attributePaths = {"user", "friend"})
    List<Friendship> findByFriend(User user);

    Optional<Friendship> findByUserAndFriend(User user, User friend);
    boolean existsByUserAndFriend(User user, User friend);
    void deleteByUserAndFriend(User user, User friend);
}


