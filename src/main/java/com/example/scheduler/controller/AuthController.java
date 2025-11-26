package com.example.scheduler.controller;

import com.example.scheduler.domain.User;
import com.example.scheduler.dto.AuthDto;
import com.example.scheduler.repository.UserRepository;
import com.example.scheduler.security.JwtTokenProvider;
import com.example.scheduler.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CookieValue;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;  // ← UserRepository 주입
    private final JwtTokenProvider jwtProvider;

    /* ---------- 회원가입 ---------- */
    @PostMapping("/signup")
    public ResponseEntity<AuthDto.SignupResponse> signup(@Valid @RequestBody AuthDto.SignupRequest req) {
        authService.signup(req);
        return ResponseEntity.ok(new AuthDto.SignupResponse("회원가입이 완료되었습니다"));
    }

    /* ---------- 로그인 ---------- */
    @PostMapping("/login")
    public ResponseEntity<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest req, HttpServletRequest request, HttpServletResponse res) {
        try {
            // 1) 액세스 토큰 발급
            String token = authService.login(req);

            // 2) 리프레시 토큰 발급 및 HttpOnly 쿠키 설정
            String refresh = jwtProvider.createRefreshToken(req.getUsername());
            addRefreshCookie(request, res, refresh);

            // 3) 유저 엔티티 조회
            User user = userRepository.findByUsername(req.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 4) 응답 DTO 반환
            AuthDto.LoginResponse resp = new AuthDto.LoginResponse(
                    token,
                    "로그인 성공",
                    user.getId(),
                    user.getNickname()
            );
            return ResponseEntity.ok(resp);

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401)
                    .body(new AuthDto.LoginResponse(null, null, null, "아이디 또는 비밀번호가 올바르지 않습니다"));
        }
    }

    /* ---------- 토큰 리프레시 ---------- */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@CookieValue(name = "refresh-token", required = false) String refreshToken,
                                                       HttpServletRequest request,
                                                       HttpServletResponse res) {
        if (refreshToken == null || !jwtProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("message", "리프레시 토큰이 유효하지 않습니다"));
        }
        String username = jwtProvider.getUsername(refreshToken);
        String newAccess = jwtProvider.createToken(username);
        // 롤링 리프레시(옵션): 만료가 임박하면 새로운 리프레시 발급
        long remains = jwtProvider.getExpiry(refreshToken).getTime() - System.currentTimeMillis();
        if (remains < Duration.ofDays(3).toMillis()) {
            String newRefresh = jwtProvider.createRefreshToken(username);
            addRefreshCookie(request, res, newRefresh);
        }
        return ResponseEntity.ok(Map.of("token", newAccess));
    }

    /* ---------- 로그아웃 ---------- */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader, HttpServletRequest request, HttpServletResponse res) {
        authService.logout(authHeader);
        // refresh 쿠키 제거
        expireRefreshCookie(request, res);
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다"));
    }

    private void addRefreshCookie(HttpServletRequest req, HttpServletResponse res, String refresh) {
        long maxAge = Math.max(0, (jwtProvider.getExpiry(refresh).getTime() - System.currentTimeMillis()) / 1000);
        boolean secure = req.isSecure() || (req.getHeader("X-Forwarded-Proto") != null && req.getHeader("X-Forwarded-Proto").startsWith("https"));
        StringBuilder sb = new StringBuilder();
        sb.append("refresh-token=").append(refresh).append("; Path=/; HttpOnly; SameSite=None; Max-Age=").append(maxAge);
        if (secure) sb.append("; Secure");
        res.addHeader("Set-Cookie", sb.toString());
    }

    private void expireRefreshCookie(HttpServletRequest req, HttpServletResponse res) {
        boolean secure = req.isSecure() || (req.getHeader("X-Forwarded-Proto") != null && req.getHeader("X-Forwarded-Proto").startsWith("https"));
        StringBuilder sb = new StringBuilder();
        sb.append("refresh-token=; Path=/; HttpOnly; SameSite=None; Max-Age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT");
        if (secure) sb.append("; Secure");
        res.addHeader("Set-Cookie", sb.toString());
    }
}
