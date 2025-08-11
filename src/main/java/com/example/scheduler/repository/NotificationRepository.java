package com.example.scheduler.repository;

import com.example.scheduler.domain.Notification;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndReadIsFalse(User user);
    void deleteByUser(User user);
    void deleteByUserAndTypeAndMessageContaining(User user, com.example.scheduler.domain.NotificationType type, String messagePart);
}


