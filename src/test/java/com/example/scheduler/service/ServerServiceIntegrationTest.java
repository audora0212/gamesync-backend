package com.example.scheduler.service;

import com.example.scheduler.common.exception.BadRequestException;
import com.example.scheduler.common.exception.ForbiddenException;
import com.example.scheduler.common.exception.NotFoundException;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.ServerDto;
import com.example.scheduler.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServerServiceIntegrationTest {

    @Mock
    private ServerRepository serverRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private TimetableEntryRepository entryRepo;

    @Mock
    private CustomGameRepository customGameRepo;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ServerInviteRepository inviteRepo;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FavoriteServerRepository favoriteRepo;

    @InjectMocks
    private ServerService serverService;

    private User testUser;
    private Server testServer;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("Test User")
                .friendCode("ABC123")
                .build();

        testServer = Server.builder()
                .id(1L)
                .name("Test Server")
                .owner(testUser)
                .members(new HashSet<>(Set.of(testUser)))
                .admins(new HashSet<>(Set.of(testUser)))
                .inviteCode("INVITE")
                .resetTime(LocalTime.of(0, 0))
                .build();

        // Mock Security Context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("서버 생성 성공")
    void createServer_Success() {
        // given
        ServerDto.CreateRequest req = new ServerDto.CreateRequest();
        req.setName("New Server");
        req.setResetTime(LocalTime.of(0, 0));

        when(serverRepo.save(any(Server.class))).thenAnswer(invocation -> {
            Server srv = invocation.getArgument(0);
            srv.setId(2L);
            return srv;
        });

        // when
        ServerDto.Response response = serverService.create(req);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("New Server");
        verify(serverRepo).save(any(Server.class));
    }

    @Test
    @DisplayName("서버 참가 성공")
    void joinServer_Success() {
        // given
        User newUser = User.builder()
                .id(2L)
                .username("newuser")
                .nickname("New User")
                .build();

        Server server = Server.builder()
                .id(1L)
                .name("Test Server")
                .owner(testUser)
                .members(new HashSet<>(Set.of(testUser)))
                .admins(new HashSet<>(Set.of(testUser)))
                .inviteCode("INVITE")
                .resetTime(LocalTime.of(0, 0))
                .build();

        when(userRepo.findByUsername("newuser")).thenReturn(Optional.of(newUser));
        when(serverRepo.findByIdWithMembers(1L)).thenReturn(Optional.of(server));
        when(serverRepo.save(any(Server.class))).thenReturn(server);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("newuser");
        SecurityContextHolder.setContext(securityContext);

        // when
        ServerDto.Response response = serverService.join(1L);

        // then
        assertThat(response).isNotNull();
        verify(serverRepo).save(any(Server.class));
    }

    @Test
    @DisplayName("서버 참가 실패 - 이미 참가한 서버")
    void joinServer_AlreadyMember_ThrowsException() {
        // given
        when(serverRepo.findByIdWithMembers(1L)).thenReturn(Optional.of(testServer));

        // when & then
        assertThatThrownBy(() -> serverService.join(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("서버 상세 조회 성공")
    void getServerDetail_Success() {
        // given
        when(serverRepo.findByIdWithMembers(1L)).thenReturn(Optional.of(testServer));

        // when
        ServerDto.Response response = serverService.getDetail(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Server");
        assertThat(response.getOwnerId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("서버 상세 조회 실패 - 멤버가 아님")
    void getServerDetail_NotMember_ThrowsException() {
        // given
        User outsider = User.builder()
                .id(2L)
                .username("outsider")
                .nickname("Outsider")
                .build();

        Server server = Server.builder()
                .id(1L)
                .name("Test Server")
                .owner(testUser)
                .members(new HashSet<>(Set.of(testUser)))
                .admins(new HashSet<>(Set.of(testUser)))
                .build();

        when(userRepo.findByUsername("outsider")).thenReturn(Optional.of(outsider));
        when(serverRepo.findByIdWithMembers(1L)).thenReturn(Optional.of(server));

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("outsider");
        SecurityContextHolder.setContext(securityContext);

        // when & then
        assertThatThrownBy(() -> serverService.getDetail(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("서버 삭제 성공")
    void deleteServer_Success() {
        // given
        when(serverRepo.findByIdWithMembers(1L)).thenReturn(Optional.of(testServer));
        when(customGameRepo.findByServer(any(Server.class))).thenReturn(java.util.Collections.emptyList());

        // when
        serverService.delete(1L);

        // then
        verify(serverRepo).delete(any(Server.class));
        verify(entryRepo).deleteAllByServer(any(Server.class));
    }

    @Test
    @DisplayName("서버 삭제 실패 - 서버장이 아님")
    void deleteServer_NotOwner_ThrowsException() {
        // given
        User notOwner = User.builder()
                .id(2L)
                .username("notowner")
                .nickname("Not Owner")
                .build();

        when(userRepo.findByUsername("notowner")).thenReturn(Optional.of(notOwner));
        when(serverRepo.findByIdWithMembers(1L)).thenReturn(Optional.of(testServer));

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("notowner");
        SecurityContextHolder.setContext(securityContext);

        // when & then
        assertThatThrownBy(() -> serverService.delete(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("서버 이름 변경 성공")
    void renameServer_Success() {
        // given
        ServerDto.UpdateNameRequest req = new ServerDto.UpdateNameRequest();
        req.setName("Updated Server Name");

        when(serverRepo.findByIdWithMembers(1L)).thenReturn(Optional.of(testServer));
        when(serverRepo.save(any(Server.class))).thenReturn(testServer);

        // when
        ServerDto.Response response = serverService.rename(1L, req);

        // then
        assertThat(response).isNotNull();
        verify(serverRepo).save(any(Server.class));
    }

    @Test
    @DisplayName("초대 코드로 서버 참가 성공")
    void joinByCode_Success() {
        // given
        User newUser = User.builder()
                .id(2L)
                .username("newuser")
                .nickname("New User")
                .build();

        Server server = Server.builder()
                .id(1L)
                .name("Test Server")
                .owner(testUser)
                .members(new HashSet<>(Set.of(testUser)))
                .admins(new HashSet<>(Set.of(testUser)))
                .inviteCode("INVITE")
                .resetTime(LocalTime.of(0, 0))
                .build();

        when(userRepo.findByUsername("newuser")).thenReturn(Optional.of(newUser));
        when(serverRepo.findByInviteCode("INVITE")).thenReturn(Optional.of(server));
        when(serverRepo.save(any(Server.class))).thenReturn(server);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("newuser");
        SecurityContextHolder.setContext(securityContext);

        // when
        ServerDto.Response response = serverService.joinByCode("INVITE");

        // then
        assertThat(response).isNotNull();
        verify(serverRepo).save(any(Server.class));
    }

    @Test
    @DisplayName("초대 코드로 서버 참가 실패 - 잘못된 코드")
    void joinByCode_InvalidCode_ThrowsException() {
        // given
        when(serverRepo.findByInviteCode("INVALID")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverService.joinByCode("INVALID"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("서버 찾기 실패 - 존재하지 않는 서버")
    void getServer_NotFound_ThrowsException() {
        // given
        when(serverRepo.findByIdWithMembers(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverService.getDetail(999L))
                .isInstanceOf(NotFoundException.class);
    }
}
