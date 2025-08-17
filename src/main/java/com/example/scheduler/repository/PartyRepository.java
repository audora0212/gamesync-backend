package com.example.scheduler.repository;

import com.example.scheduler.domain.Party;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartyRepository extends JpaRepository<Party, Long> {
    List<Party> findByServerOrderBySlotAsc(Server server);
    List<Party> findByParticipantsContaining(User user);
}


