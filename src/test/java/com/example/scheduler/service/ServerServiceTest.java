package com.example.scheduler.service;

import com.example.scheduler.common.exception.BadRequestException;
import com.example.scheduler.common.exception.NotFoundException;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.ServerDto;
import com.example.scheduler.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerServiceTest {

    @Mock
    private ServerRepository serverRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private TimetableEntryRepository entryRepo;

    @Mock
    private PartyRepository partyRepo;

    @Mock
    private AuditService auditService;

    @Mock
    private FavoriteServerRepository favoriteRepo;

    @Mock
    private ServerInviteRepository inviteRepo;

    @InjectMocks
    private ServerService serverService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(String username) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("서버 생성 성공")
    void create_Success() {
        // given
        setupSecurityContext("testuser");

        User owner = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("Test User")
                .build();

        ServerDto.CreateRequest request = new ServerDto.CreateRequest();
        request.setName("New Server");

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(owner));
        when(serverRepo.save(any(Server.class))).thenAnswer(invocation -> {
            Server server = invocation.getArgument(0);
            server.setId(2L);
            return server;
        });

        // when
        ServerDto.Response response = serverService.create(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("New Server");
        verify(serverRepo).save(any(Server.class));
    }

    @Test
    @DisplayName("서버 참가 실패 - 서버 없음")
    void join_ServerNotFound() {
        // given
        setupSecurityContext("testuser");

        User user = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("Test User")
                .build();

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(serverRepo.findByIdWithMembers(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverService.join(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("초대 코드로 서버 조회")
    void lookupByCode_Success() {
        // given
        User owner = User.builder()
                .id(1L)
                .username("owner")
                .nickname("Owner")
                .build();

        Server server = Server.builder()
                .id(1L)
                .name("Test Server")
                .owner(owner)
                .members(new HashSet<>(Set.of(owner)))
                .admins(new HashSet<>(Set.of(owner)))
                .inviteCode("TEST01")
                .resetTime(LocalTime.of(5, 0))
                .build();

        when(serverRepo.findByInviteCode("TEST01")).thenReturn(Optional.of(server));

        // when
        ServerDto.Response response = serverService.lookupByCode("TEST01");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Server");
    }

    @Test
    @DisplayName("초대 코드로 서버 조회 실패 - 서버 없음")
    void lookupByCode_NotFound() {
        // given
        when(serverRepo.findByInviteCode("INVALID")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverService.lookupByCode("INVALID"))
                .isInstanceOf(BadRequestException.class);
    }
}
