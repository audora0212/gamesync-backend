package com.example.scheduler.service;

import com.example.scheduler.domain.User;
import com.example.scheduler.dto.UserDto;
import com.example.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDto.Profile getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new UserDto.Profile(user.getId(), user.getUsername(), user.getNickname(), user.getEmail());
    }

    public UserDto.Profile updateNickname(String username, String newNickname) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setNickname(newNickname);
        userRepository.save(user);
        return new UserDto.Profile(user.getId(), user.getUsername(), user.getNickname(), user.getEmail());
    }
}