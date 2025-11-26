package com.example.scheduler.service;

import com.example.scheduler.domain.User;
import com.example.scheduler.dto.AuthDto;
import com.example.scheduler.repository.BlacklistedTokenRepository;
import com.example.scheduler.repository.UserRepository;
import com.example.scheduler.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtTokenProvider jwtProvider;

    @Mock
    private FriendCodeService friendCodeService;

    @Mock
    private BlacklistedTokenRepository blacklistRepo;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        // given
        AuthDto.SignupRequest req = new AuthDto.SignupRequest();
        req.setUsername("testuser");
        req.setPassword("password123");
        req.setNickname("Test User");

        when(userRepo.existsByUsername("testuser")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("encodedPassword");
        when(friendCodeService.generateUniqueFriendCode()).thenReturn("ABC123");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        authService.signup(req);

        // then
        verify(userRepo).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 아이디")
    void signup_DuplicateUsername_ThrowsException() {
        // given
        AuthDto.SignupRequest req = new AuthDto.SignupRequest();
        req.setUsername("testuser");
        req.setPassword("password123");
        req.setNickname("Test User");

        when(userRepo.existsByUsername("testuser")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 사용 중인 아이디입니다");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtProvider.createToken("testuser")).thenReturn("jwt-token");

        // when
        String token = authService.login(req);

        // then
        assertThat(token).isEqualTo("jwt-token");
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("로그아웃 실패 - 잘못된 Authorization 헤더")
    void logout_InvalidHeader_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> authService.logout("InvalidToken"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authorization 헤더가 필요합니다");
    }

    @Test
    @DisplayName("로그아웃 실패 - null Authorization 헤더")
    void logout_NullHeader_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authorization 헤더가 필요합니다");
    }
}
