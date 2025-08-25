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
import java.util.Arrays;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtProvider;
    private final UserRepository    userRepo;
    private final ObjectMapper      objectMapper = new ObjectMapper();

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.frontend.discord-callback-path:/auth/discord/callback}")
    private String discordCallbackPath;

    @Value("${app.frontend.kakao-callback-path:/auth/kakao/callback}")
    private String kakaoCallbackPath;

    @Value("${app.ios.scheme:gamesync://}")
    private String iosScheme;

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

        // oauth_target 쿠키 값(app | mobile-web | web)에 따라 최종 목적지를 분기
        String oauthTarget = null;
        if (req.getCookies() != null) {
            oauthTarget = Arrays.stream(req.getCookies())
                    .filter(c -> "oauth_target".equals(c.getName()))
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        // Debug: UA / cookie dump
        String uaHdr = req.getHeader("User-Agent");
        System.out.println("[OAuth2Success] UA=" + uaHdr);
        System.out.println("[OAuth2Success] oauth_target_cookie=" + oauthTarget);

        // 기본: 웹 콜백 (단, 모바일 UA에서는 스킴을 기본으로 폴백)
        String finalUrl = String.format("%s%s?token=%s&user=%s", frontendBaseUrl, callbackPath, token, encodedUser);

        if (oauthTarget != null) {
            switch (oauthTarget) {
                case "app":
                    // SFSafariViewController는 Universal Links로 앱 전환을 허용하지 않음
                    // → 커스텀 스킴으로 확실히 앱으로 복귀(appUrlOpen 이벤트 유발)
                    finalUrl = String.format("%s/auth/%s/callback?token=%s&user=%s",
                            iosScheme.replaceAll("/$", ""),
                            provider,
                            token,
                            encodedUser);
                    break;
                case "mobile-web":
                    // 웹 콜백 유지: 프론트에서 앱 열기 시도 후 스토어 폴백 처리
                    break;
                case "web":
                default:
                    break;
            }
        } else {
            // 쿠키가 없는 경우: iOS/모바일 UA면 스킴으로 폴백
            String ua = req.getHeader("User-Agent");
            if (ua != null && (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod") || ua.contains("Mobile") || ua.contains("Safari"))) {
                finalUrl = String.format("%s/auth/%s/callback?token=%s&user=%s",
                        iosScheme.replaceAll("/$", ""),
                        provider,
                        token,
                        encodedUser);
            }
        }

        // 최종 리다이렉트 로그 (결정 이후)
        System.out.println("[OAuth2Success] provider=" + provider + " userId=" + user.getId());
        System.out.println("[OAuth2Success] target=" + oauthTarget + " finalRedirect=" + finalUrl);

        // 사용 후 쿠키 만료(클라이언트 지시)
        jakarta.servlet.http.Cookie expired = new jakarta.servlet.http.Cookie("oauth_target", "");
        expired.setMaxAge(0);
        expired.setPath("/");
        // SameSite, Secure 등은 Set-Cookie 헤더에 추가해야 하지만 표준 Cookie API로는 제한적
        res.addCookie(expired);

        res.sendRedirect(finalUrl);
    }
}
