package com.example.scheduler.security;

import com.example.scheduler.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    private final JwtTokenProvider tokenProvider;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private final CustomOAuth2UserService oauth2UserService;
    private final OAuth2LoginSuccessHandler oauth2SuccessHandler;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    public SecurityConfig(JwtTokenProvider tokenProvider,
                          org.springframework.security.core.userdetails.UserDetailsService userDetailsService,
                          CustomOAuth2UserService oauth2UserService,
                          OAuth2LoginSuccessHandler oauth2SuccessHandler,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.oauth2UserService = oauth2UserService;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // JWT 필터
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(tokenProvider, userDetailsService);

        http
                // CORS
                .cors(Customizer.withDefaults())
                // CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 세션을 사용하지 않음
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // URL 별 인가 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error",
                                "/healthcheck"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 등록
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // OAuth2 클라이언트 설정이 있을 때만 oauth2Login 구성
        ClientRegistrationRepository clientRepo = clientRegistrationRepositoryProvider.getIfAvailable();
        if (clientRepo != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/auth/login")
                    .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                    .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                    .userInfoEndpoint(u -> u.userService(oauth2UserService))
                    .successHandler(oauth2SuccessHandler)
            );
        }

        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 단일 origin, 혹은 콤마(,) 구분으로 여러 개
        cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // 비밀번호 암호화
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager 빈
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
