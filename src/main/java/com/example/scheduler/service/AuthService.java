package com.example.scheduler.service;

import com.example.scheduler.domain.BlacklistedToken;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.AuthDto;
import com.example.scheduler.repository.BlacklistedTokenRepository;
import com.example.scheduler.repository.UserRepository;
import com.example.scheduler.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtProvider;

    private final BlacklistedTokenRepository blacklistRepo;

    /* ---------- 회원가입 & 로그인 ---------- */

    public void signup(AuthDto.SignupRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "이미 사용 중인 아이디입니다");
        }
        User user = User.builder()
                .username(req.getUsername())
                .nickname(req.getNickname())
                .password(encoder.encode(req.getPassword()))
                .friendCode(generateUniqueFriendCode()) // ← 6자리 친구코드 부여
                .build();
        userRepo.save(user);
    }

    public String login(AuthDto.LoginRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        return jwtProvider.createToken(auth.getName());
    }

    /* ---------- 로그아웃 ---------- */
    public void logout(String bearerHeader) {
        if (bearerHeader == null || !bearerHeader.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authorization 헤더가 필요합니다");

        String token = bearerHeader.substring(7);

        // 이미 무효 처리된 토큰인지 점검
        if (!jwtProvider.validateToken(token))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다");

        Date expiry = jwtProvider.getExpiry(token);

        blacklistRepo.save(
                BlacklistedToken.builder()
                        .token(token)
                        .expiry(expiry)
                        .build());
    }

    /* ---------- 친구코드 유틸 ---------- */
    private String generateUniqueFriendCode() {
        // 000000 ~ 999999 (0 채움) 중에서 유니크한 코드가 나올 때까지 시도
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        for (int attempt = 0; attempt < 50; attempt++) { // 너무 오래 돌지 않게 제한
            int n = rnd.nextInt(1_000_000);
            String code = String.format("%06d", n);
            if (!userRepo.existsByFriendCode(code)) return code;
        }
        // 이 정도면 사실상 안 나오기 힘든데, 혹시 몰라 에러 처리
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "친구코드 생성 실패. 잠시 후 다시 시도");
    }


}
