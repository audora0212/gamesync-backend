package com.example.scheduler.security;

import com.example.scheduler.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String ISSUER = "gamesync";

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
        // 키 길이 검증 (HS512는 최소 64바이트 권장)
        if (keyBytes.length < 32) {
            log.warn("JWT secret key length is less than 256 bits. Consider using a stronger key.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /* ---------- 발행 ---------- */
    public String createToken(String username) {
        return createToken(username, validityInMs, TOKEN_TYPE_ACCESS);
    }

    public String createRefreshToken(String username) {
        return createToken(username, refreshValidityInMs, TOKEN_TYPE_REFRESH);
    }

    private String createToken(String username, long ttlMs, String tokenType) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())  // 고유 토큰 ID
                .setSubject(username)
                .setIssuer(ISSUER)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(key, SignatureAlgorithm.HS512)  // HS256 → HS512
                .compact();
    }

    /* ---------- 파싱 유틸 ---------- */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Date getExpiry(String token) {
        return getClaims(token).getExpiration();
    }

    public String getTokenId(String token) {
        return getClaims(token).getId();
    }

    public String getTokenType(String token) {
        return getClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    private Claims getClaims(String token) {
        return parser().parseClaimsJws(token).getBody();
    }

    private JwtParser parser() {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(ISSUER)  // issuer 검증
                .build();
    }

    /* ---------- 검증 ---------- */
    public boolean validateToken(String token) {
        return validateToken(token, TOKEN_TYPE_ACCESS);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, TOKEN_TYPE_REFRESH);
    }

    private boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = getClaims(token);

            // 토큰 타입 검증
            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (tokenType != null && !tokenType.equals(expectedType)) {
                log.debug("Token type mismatch: expected={}, actual={}", expectedType, tokenType);
                return false;
            }

            // 블랙리스트 검사
            if (blacklistRepo.existsByToken(token)) {
                log.debug("Token is blacklisted");
                return false;
            }

            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
