package com.example.scheduler.repository;

import com.example.scheduler.domain.FriendNotificationSetting;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface FriendNotificationSettingRepository extends JpaRepository<FriendNotificationSetting, Long> {
    Optional<FriendNotificationSetting> findByOwnerAndFriend(User owner, User friend);
    List<FriendNotificationSetting> findByOwner(User owner);
}


