package com.example.scheduler.security;

import com.example.scheduler.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretBase64;

    @Value("${jwt.expiration-ms}")
    private long validityInMs;

    @Value("${jwt.refresh-expiration-ms:1209600000}") // 기본 14일
    private long refreshValidityInMs;

    private final BlacklistedTokenRepository blacklistRepo;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretBase64);
        } catch (IllegalArgumentException e) {
            // Fallback: treat as plain text secret when not Base64-encoded
            keyBytes = secretBase64.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /* ---------- 발행 ---------- */
    public String createToken(String username) {
        return createToken(username, validityInMs);
    }

    public String createRefreshToken(String username) {
        return createToken(username, refreshValidityInMs);
    }

    private String createToken(String username, long ttlMs) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ---------- 파싱 유틸 ---------- */
    public String getUsername(String token) {
        return parser().parseClaimsJws(token).getBody().getSubject();
    }

    public Date getExpiry(String token) {
        return parser().parseClaimsJws(token).getBody().getExpiration();
    }

    private JwtParser parser() {
        return Jwts.parserBuilder().setSigningKey(key).build();
    }

    /* ---------- 검증 ---------- */
    public boolean validateToken(String token) {
        try {
            parser().parseClaimsJws(token);            // 서명·만료 검사
            return !blacklistRepo.existsByToken(token); // 블랙리스트 검사
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
