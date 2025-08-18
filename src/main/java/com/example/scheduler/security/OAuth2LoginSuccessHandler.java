// security/OAuth2LoginSuccessHandler.java
package com.example.scheduler.security;

import com.example.scheduler.domain.User;
import com.example.scheduler.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.frontend.discord-callback-path}")
    private String discordCallbackPath;

    @Value("${app.frontend.kakao-callback-path}")
    private String kakaoCallbackPath;

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
        String provider = oauthUser.getAttribute("provider");
        String externalId = oauthUser.getAttribute("id");

        User user;
        if ("kakao".equalsIgnoreCase(provider)) {
            user = userRepo.findByKakaoId(externalId).orElseThrow();
        } else {
            user = userRepo.findByDiscordId(externalId).orElseThrow();
            provider = "discord";
        }

        String token = jwtProvider.createToken(user.getUsername());

        Map<String,Object> payload = Map.of(
                "id",       user.getId(),
                "nickname", user.getNickname()
        );
        String userJson    = objectMapper.writeValueAsString(payload);
        String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);

        // properties 로 뺀 값 사용
        String callbackPath = "discord".equalsIgnoreCase(provider) ? discordCallbackPath : kakaoCallbackPath;
        String redirectUrl = String.format(
                "%s%s?token=%s&user=%s",
                frontendBaseUrl,
                callbackPath,
                token,
                encodedUser
        );
        res.sendRedirect(redirectUrl);
    }
}
