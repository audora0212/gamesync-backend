package com.example.scheduler.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * /oauth2/authorization/* 요청에 대해 target 파라미터(app|mobile-web|web)를 감지하여
 * 같은 도메인의 1P 쿠키로 저장한다. (Safari(WebAuth)와의 쿠키 분리 문제 해결)
 */
public class OAuthTargetCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/oauth2/authorization")) {
            String target = request.getParameter("target");
            if (target != null && !target.isBlank()) {
                Cookie cookie = new Cookie("oauth_target", target);
                cookie.setPath("/");
                cookie.setMaxAge(300); // 5분
                // Secure/SameSite 설정은 서버/프록시에서 헤더로 보강될 수 있음
                response.addCookie(cookie);
            }
        }
        filterChain.doFilter(request, response);
    }
}


