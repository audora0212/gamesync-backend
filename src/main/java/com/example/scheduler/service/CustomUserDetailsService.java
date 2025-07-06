package com.example.scheduler.service;

import com.example.scheduler.domain.User;
import com.example.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrDiscordId)
            throws UsernameNotFoundException {
        // 1) 우선 일반 username 으로 시도
        Optional<User> opt = userRepository.findByUsername(usernameOrDiscordId);

        // 2) 못 찾으면 discordId 로도 시도
        User user = opt.orElseGet(() ->
                userRepository.findByDiscordId(usernameOrDiscordId)
                        .orElseThrow(() ->
                                new UsernameNotFoundException("User not found: " + usernameOrDiscordId)
                        )
        );

        // Spring Security 용 UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),                // 로그인 시 비밀번호 검증은 필요 없지만
                user.getPassword() == null
                        ? "" : user.getPassword(),     // null 방지
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_USER")
                )
        );
    }
}