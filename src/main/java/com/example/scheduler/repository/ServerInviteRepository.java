package com.example.scheduler.repository;

import com.example.scheduler.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServerInviteRepository extends JpaRepository<ServerInvite, Long> {
    List<ServerInvite> findByReceiverAndStatus(User receiver, InviteStatus status);
    List<ServerInvite> findBySender(User sender);
    Optional<ServerInvite> findByServerAndSenderAndReceiver(Server server, User sender, User receiver);
}


