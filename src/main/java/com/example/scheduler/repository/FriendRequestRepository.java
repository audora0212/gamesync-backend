package com.example.scheduler.repository;

import com.example.scheduler.domain.FriendRequest;
import com.example.scheduler.domain.FriendRequestStatus;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

    // 친구 요청 목록 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<FriendRequest> findByReceiverAndStatus(User receiver, FriendRequestStatus status);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<FriendRequest> findBySenderAndStatus(User sender, FriendRequestStatus status);
}


