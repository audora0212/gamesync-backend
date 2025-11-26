package com.example.scheduler.service;

import com.example.scheduler.domain.*;
import com.example.scheduler.dto.PartyDto;
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
class PartyServiceTest {

    @Mock
    private PartyRepository partyRepo;

    @Mock
    private ServerRepository serverRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private DefaultGameRepository defaultGameRepo;

    @Mock
    private CustomGameRepository customGameRepo;

    @Mock
    private TimetableEntryRepository entryRepo;

    @Mock
    private TimetableService timetableService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PartyService partyService;

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
    @DisplayName("파티 목록 조회")
    void list_Success() {
        // given
        User creator = User.builder().id(1L).username("creator").nickname("Creator").build();
        Server server = Server.builder().id(1L).name("Server").build();
        DefaultGame game = DefaultGame.builder().id(1L).name("VALORANT").build();
        Party party = Party.builder()
                .id(1L).server(server).creator(creator).defaultGame(game)
                .slot(LocalDateTime.now().plusHours(2)).capacity(5)
                .participants(new HashSet<>(Set.of(creator))).build();

        when(serverRepo.findById(1L)).thenReturn(Optional.of(server));
        when(partyRepo.findByServerOrderBySlotAsc(server)).thenReturn(List.of(party));

        // when
        List<PartyDto.Response> responses = partyService.list(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCapacity()).isEqualTo(5);
    }

    @Test
    @DisplayName("파티 목록 조회 실패 - 서버 없음")
    void list_ServerNotFound() {
        // given
        when(serverRepo.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> partyService.list(999L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("파티 참가 실패 - 파티 정원 초과")
    void join_PartyFull_ThrowsException() {
        // given
        setupSecurityContext("newuser");

        User creator = User.builder().id(1L).username("creator").nickname("Creator").build();
        User newUser = User.builder().id(2L).username("newuser").nickname("New").build();
        Server server = Server.builder().id(1L).name("Server").build();
        DefaultGame game = DefaultGame.builder().id(1L).name("VALORANT").build();

        // 파티 정원 1명이고 이미 생성자가 참가 중
        Party party = Party.builder()
                .id(1L).server(server).creator(creator).defaultGame(game)
                .slot(LocalDateTime.now().plusHours(2)).capacity(1)
                .participants(new HashSet<>(Set.of(creator))).build();

        when(userRepo.findByUsername("newuser")).thenReturn(Optional.of(newUser));
        when(partyRepo.findById(1L)).thenReturn(Optional.of(party));
        when(partyRepo.findByParticipantsContaining(newUser)).thenReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> partyService.join(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("full");
    }

    @Test
    @DisplayName("파티 참가 - 이미 참가 중일 때 그대로 반환")
    void join_AlreadyParticipant_ReturnsParty() {
        // given
        setupSecurityContext("testuser");

        User user = User.builder().id(1L).username("testuser").nickname("Test").build();
        Server server = Server.builder().id(1L).name("Server").build();
        DefaultGame game = DefaultGame.builder().id(1L).name("VALORANT").build();
        Party party = Party.builder()
                .id(1L).server(server).creator(user).defaultGame(game)
                .slot(LocalDateTime.now().plusHours(2)).capacity(5)
                .participants(new HashSet<>(Set.of(user))).build();

        when(userRepo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(partyRepo.findById(1L)).thenReturn(Optional.of(party));
        when(partyRepo.findByParticipantsContaining(user)).thenReturn(List.of(party));

        // when
        PartyDto.Response response = partyService.join(1L);

        // then - 이미 참가 중이면 그냥 반환
        assertThat(response).isNotNull();
        assertThat(response.getCapacity()).isEqualTo(5);
    }

    @Test
    @DisplayName("파티 삭제 - 생성자 아닌 경우 실패")
    void deleteParty_NotCreator_ThrowsException() {
        // given
        setupSecurityContext("otheruser");

        User creator = User.builder().id(1L).username("creator").nickname("Creator").build();
        User other = User.builder().id(2L).username("otheruser").nickname("Other").build();
        Server server = Server.builder().id(1L).name("Server").build();
        DefaultGame game = DefaultGame.builder().id(1L).name("VALORANT").build();
        Party party = Party.builder()
                .id(1L).server(server).creator(creator).defaultGame(game)
                .slot(LocalDateTime.now().plusHours(2)).capacity(5)
                .participants(new HashSet<>(Set.of(creator, other))).build();

        when(userRepo.findByUsername("otheruser")).thenReturn(Optional.of(other));
        when(partyRepo.findById(1L)).thenReturn(Optional.of(party));

        // when & then
        assertThatThrownBy(() -> partyService.deleteParty(1L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
