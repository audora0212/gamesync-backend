// security/OAuth2LoginSuccessHandler.java
package com.example.scheduler.security;

import com.example.scheduler.domain.User;
import com.example.scheduler.repository.UserRepository;
import com.example.scheduler.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtProvider;
    private final UserRepository    userRepo;
    private final ObjectMapper      objectMapper = new ObjectMapper();

    public OAuth2LoginSuccessHandler(JwtTokenProvider jwtProvider,
                                     UserRepository userRepo) {
        this.jwtProvider = jwtProvider;
        this.userRepo    = userRepo;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest  req,
            HttpServletResponse res,
            Authentication      auth) throws IOException {

        OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
        String discordId     = oauthUser.getAttribute("id");

        // DB에서 User 조회
        User user = userRepo.findByDiscordId(discordId)
                .orElseThrow();

        // JWT 발급 (subject = username)
        String token    = jwtProvider.createToken(user.getUsername());

        // 전달할 사용자 정보(JSON)
        Map<String, Object> payload = Map.of(
                "id",       user.getId(),
                "nickname", user.getNickname()
        );
        String userJson    = objectMapper.writeValueAsString(payload);
        String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);

        // 프론트로 리다이렉트: 토큰과 인코딩된 JSON 전달
        String redirectUrl = String.format(
                "http://localhost:3000/auth/discord/callback?token=%s&user=%s",
                token, encodedUser
        );
        res.sendRedirect(redirectUrl);
    }
}
