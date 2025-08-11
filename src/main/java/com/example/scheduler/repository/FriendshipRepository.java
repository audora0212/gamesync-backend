package com.example.scheduler.repository;

import com.example.scheduler.domain.Friendship;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findByUser(User user);
    List<Friendship> findByFriend(User user);
    Optional<Friendship> findByUserAndFriend(User user, User friend);
    boolean existsByUserAndFriend(User user, User friend);
}


