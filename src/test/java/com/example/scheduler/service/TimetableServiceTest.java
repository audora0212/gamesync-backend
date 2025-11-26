package com.example.scheduler.service;

import com.example.scheduler.domain.*;
import com.example.scheduler.dto.TimetableDto;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TimetableEntryRepository entryRepo;

    @Mock
    private ServerRepository serverRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private DefaultGameRepository defaultGameRepo;

    @Mock
    private CustomGameRepository customGameRepo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TimetableService timetableService;

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
    @DisplayName("스케줄 등록 성공")
    void add_Success() {
        // given
        setupSecurityContext("testuser");

        User user = User.builder().id(1L).username("testuser").nickname("Test").build();
        Server server = Server.builder().id(1L).name("Server").members(new HashSet<>(Set.of(user))).build();
        DefaultGame game = DefaultGame.builder().id(1L).name("VALORANT").build();

        TimetableDto.EntryRequest request = new TimetableDto.EntryRequest();
        request.setServerId(1L);
        request.setDefaultGameId(1L);
        request.setSlot(LocalDateTime.now().plusHours(2));

        TimetableEntry entry = TimetableEntry.builder()
                .id(1L).server(server).user(user).defaultGame(game)
                .slot(request.getSlot()).build();

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(serverRepo.findById(1L)).thenReturn(Optional.of(server));
        when(partyRepository.existsByServerAndParticipantsContaining(any(), any())).thenReturn(false);
        when(entryRepo.findByServerAndUser(any(), any())).thenReturn(Optional.empty());
        when(defaultGameRepo.findById(1L)).thenReturn(Optional.of(game));
        when(entryRepo.save(any(TimetableEntry.class))).thenReturn(entry);
        when(friendshipRepository.findByUser(any())).thenReturn(Collections.emptyList());
        when(friendshipRepository.findByFriend(any())).thenReturn(Collections.emptyList());

        // when
        TimetableDto.EntryResponse response = timetableService.add(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getGameName()).isEqualTo("VALORANT");
        verify(entryRepo).save(any(TimetableEntry.class));
    }

    @Test
    @DisplayName("스케줄 등록 실패 - 파티 참가 중")
    void add_InParty_ThrowsException() {
        // given
        setupSecurityContext("testuser");

        User user = User.builder().id(1L).username("testuser").nickname("Test").build();
        Server server = Server.builder().id(1L).name("Server").build();

        TimetableDto.EntryRequest request = new TimetableDto.EntryRequest();
        request.setServerId(1L);
        request.setDefaultGameId(1L);
        request.setSlot(LocalDateTime.now().plusHours(2));

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(serverRepo.findById(1L)).thenReturn(Optional.of(server));
        when(partyRepository.existsByServerAndParticipantsContaining(server, user)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> timetableService.add(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("파티에 참가 중입니다");
    }

    @Test
    @DisplayName("스케줄 등록 실패 - 게임 ID 없음")
    void add_NoGameId_ThrowsException() {
        // given
        setupSecurityContext("testuser");

        User user = User.builder().id(1L).username("testuser").nickname("Test").build();
        Server server = Server.builder().id(1L).name("Server").build();

        TimetableDto.EntryRequest request = new TimetableDto.EntryRequest();
        request.setServerId(1L);
        request.setSlot(LocalDateTime.now().plusHours(2));
        // No game ID set

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(serverRepo.findById(1L)).thenReturn(Optional.of(server));
        when(partyRepository.existsByServerAndParticipantsContaining(any(), any())).thenReturn(false);
        when(entryRepo.findByServerAndUser(any(), any())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> timetableService.add(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Game ID is required");
    }

    @Test
    @DisplayName("스케줄 목록 조회")
    void list_Success() {
        // given
        User user = User.builder().id(1L).username("testuser").nickname("Test").build();
        Server server = Server.builder().id(1L).name("Server").build();
        DefaultGame game = DefaultGame.builder().id(1L).name("VALORANT").build();
        TimetableEntry entry = TimetableEntry.builder()
                .id(1L).server(server).user(user).defaultGame(game)
                .slot(LocalDateTime.now().plusHours(2)).build();

        when(serverRepo.findById(1L)).thenReturn(Optional.of(server));
        when(entryRepo.findByServerOrderBySlot(server)).thenReturn(List.of(entry));

        // when
        List<TimetableDto.EntryResponse> responses = timetableService.list(1L, null, false);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getGameName()).isEqualTo("VALORANT");
    }

    @Test
    @DisplayName("스케줄 목록 조회 - 게임 필터링")
    void list_WithGameFilter() {
        // given
        User user = User.builder().id(1L).username("testuser").nickname("Test").build();
        Server server = Server.builder().id(1L).name("Server").build();
        DefaultGame game = DefaultGame.builder().id(1L).name("VALORANT").build();
        TimetableEntry entry = TimetableEntry.builder()
                .id(1L).server(server).user(user).defaultGame(game)
                .slot(LocalDateTime.now().plusHours(2)).build();

        when(serverRepo.findById(1L)).thenReturn(Optional.of(server));
        when(entryRepo.findByServerOrderBySlot(server)).thenReturn(List.of(entry));

        // when
        List<TimetableDto.EntryResponse> responses = timetableService.list(1L, "VALORANT", false);

        // then
        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("스케줄 목록 조회 - 다른 게임 필터링 시 빈 결과")
    void list_WithOtherGameFilter_Empty() {
        // given
        User user = User.builder().id(1L).username("testuser").nickname("Test").build();
        Server server = Server.builder().id(1L).name("Server").build();
        DefaultGame game = DefaultGame.builder().id(1L).name("VALORANT").build();
        TimetableEntry entry = TimetableEntry.builder()
                .id(1L).server(server).user(user).defaultGame(game)
                .slot(LocalDateTime.now().plusHours(2)).build();

        when(serverRepo.findById(1L)).thenReturn(Optional.of(server));
        when(entryRepo.findByServerOrderBySlot(server)).thenReturn(List.of(entry));

        // when
        List<TimetableDto.EntryResponse> responses = timetableService.list(1L, "LOL", false);

        // then
        assertThat(responses).isEmpty();
    }
}
