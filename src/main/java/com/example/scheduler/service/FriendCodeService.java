package com.example.scheduler.service;

import com.example.scheduler.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FriendCodeService {

    private final UserRepository userRepository;

    public FriendCodeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateUniqueFriendCode() {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        for (int attempt = 0; attempt < 50; attempt++) {
            int randomNumber = secureRandom.nextInt(1_000_000);
            String candidate = String.format("%06d", randomNumber);
            if (!userRepository.existsByFriendCode(candidate)) {
                return candidate;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "친구코드 생성 실패. 잠시 후 다시 시도"
        );
    }
}


