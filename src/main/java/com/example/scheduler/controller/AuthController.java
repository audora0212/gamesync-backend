package com.example.scheduler.controller;

import com.example.scheduler.domain.User;
import com.example.scheduler.dto.AuthDto;
import com.example.scheduler.repository.UserRepository;
import com.example.scheduler.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;  // ← UserRepository 주입

    /* ---------- 회원가입 ---------- */
    @PostMapping("/signup")
    public ResponseEntity<AuthDto.SignupResponse> signup(@RequestBody AuthDto.SignupRequest req) {
        authService.signup(req);
        return ResponseEntity.ok(new AuthDto.SignupResponse("회원가입이 완료되었습니다"));
    }

    /* ---------- 로그인 ---------- */
    @PostMapping("/login")
    public ResponseEntity<AuthDto.LoginResponse> login(@RequestBody AuthDto.LoginRequest req) {
        try {
            // 1) 토큰 발급
            String token = authService.login(req);

            // 2) 유저 엔티티 조회
            User user = userRepository.findByUsername(req.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3) 응답 DTO에 토큰, 아이디, 닉네임, 메시지 담아서 반환
            AuthDto.LoginResponse resp = new AuthDto.LoginResponse(
                    token,
                    "로그인 성공",       // message
                    user.getId(),        // userId
                    user.getNickname()   // nickname
            );
            return ResponseEntity.ok(resp);

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401)
                    .body(new AuthDto.LoginResponse(null, null, null, "아이디 또는 비밀번호가 올바르지 않습니다"));
        }
    }

    /* ---------- 로그아웃 ---------- */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다"));
    }

}
