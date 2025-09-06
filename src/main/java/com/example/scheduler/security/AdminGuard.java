package com.example.scheduler.security;

import com.example.scheduler.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component("adminGuard")
public class AdminGuard {
    private final UserRepository userRepository;

    public AdminGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin(String username) {
        if (username == null) return false;
        return userRepository.findByUsername(username)
                .map(u -> Boolean.TRUE.equals(u.getAdmin()))
                .orElse(false);
    }
}


