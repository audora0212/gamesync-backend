package com.example.scheduler.service;

import com.example.scheduler.common.exception.BadRequestException;
import com.example.scheduler.common.exception.ErrorCode;
import com.example.scheduler.domain.BlacklistedToken;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.AuthDto;
import com.example.scheduler.repository.BlacklistedTokenRepository;
import com.example.scheduler.repository.UserRepository;
import com.example.scheduler.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtProvider;
    private final FriendCodeService friendCodeService;

    private final BlacklistedTokenRepository blacklistRepo;
    

    /* ---------- 회원가입 & 로그인 ---------- */

    @org.springframework.transaction.annotation.Transactional
    public void signup(AuthDto.SignupRequest req) {
        log.info("Signup attempt: username={}", req.getUsername());

        if (userRepo.existsByUsername(req.getUsername())) {
            log.warn("Signup failed - username already exists: username={}", req.getUsername());
            throw new BadRequestException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User user = User.builder()
                .username(req.getUsername())
                .nickname(req.getNickname())
                .password(encoder.encode(req.getPassword()))
                .admin(false)
                .friendCode(friendCodeService.generateUniqueFriendCode())
                .build();
        userRepo.save(user);
        log.info("Signup successful: username={}, userId={}", req.getUsername(), user.getId());
    }

    public String login(AuthDto.LoginRequest req) {
        log.info("Login attempt: username={}", req.getUsername());
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        String token = jwtProvider.createToken(auth.getName());
        log.info("Login successful: username={}", req.getUsername());
        return token;
    }

    /* ---------- 로그아웃 ---------- */
    @org.springframework.transaction.annotation.Transactional
    public void logout(String bearerHeader) {
        if (bearerHeader == null || !bearerHeader.startsWith("Bearer ")) {
            log.warn("Logout failed - invalid authorization header");
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "Authorization 헤더가 필요합니다");
        }

        String token = bearerHeader.substring(7);

        // 이미 무효 처리된 토큰인지 점검
        if (!jwtProvider.validateToken(token)) {
            log.warn("Logout failed - invalid token");
            throw new BadRequestException(ErrorCode.TOKEN_INVALID);
        }

        String username = jwtProvider.getUsername(token);
        Date expiry = jwtProvider.getExpiry(token);

        blacklistRepo.save(
                BlacklistedToken.builder()
                        .token(token)
                        .expiry(expiry)
                        .build());

        log.info("Logout successful: username={}", username);
    }

    
}
