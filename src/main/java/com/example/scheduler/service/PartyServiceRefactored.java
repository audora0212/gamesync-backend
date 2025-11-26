package com.example.scheduler.service;

import com.example.scheduler.common.base.BaseService;
import com.example.scheduler.common.constant.AuditAction;
import com.example.scheduler.common.exception.BadRequestException;
import com.example.scheduler.common.exception.ForbiddenException;
import com.example.scheduler.common.exception.NotFoundException;
import com.example.scheduler.domain.*;
import com.example.scheduler.dto.PartyDto;
import com.example.scheduler.dto.TimetableDto;
import com.example.scheduler.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 파티 관련 비즈니스 로직
 * BaseService를 상속받아 공통 기능 활용
 */
@Service("partyServiceRefactored")
@RequiredArgsConstructor
public class PartyServiceRefactored extends BaseService {

    private final PartyRepository partyRepo;
    private final ServerRepository serverRepo;
    private final DefaultGameRepository defaultGameRepo;
    private final CustomGameRepository customGameRepo;
    private final TimetableService timetableService;
    private final NotificationService notificationService;
    private final TimetableEntryRepository timetableEntryRepo;
    private final AuditService auditService;

    // ==================== 파티 생성 ====================

    /**
     * 파티 생성
     */
    @Transactional
    public PartyDto.Response create(PartyDto.CreateRequest req) {
        User creator = getCurrentUser();
        Server server = findServerById(req.getServerId());

        Party party = buildParty(req, creator, server);
        Party saved = partyRepo.save(party);

        auditPartyAction(server.getId(), creator.getId(), AuditAction.CREATE_PARTY, saved);

        // 생성자의 타임테이블 등록
        addTimetableEntry(saved);

        // 생성자 자동 참가
        saved.getParticipants().add(creator);
        auditPartyAction(server.getId(), creator.getId(), AuditAction.JOIN_PARTY, saved);

        // 서버 멤버들에게 알림 발송
        notifyPartyCreated(server, creator, saved);

        return toResponse(saved);
    }

    // ==================== 파티 목록 조회 ====================

    /**
     * 서버의 파티 목록 조회
     */
    public List<PartyDto.Response> list(Long serverId) {
        Server server = findServerById(serverId);

        return partyRepo.findByServerOrderBySlotAsc(server).stream()
                .map(this::toResponse)
                .toList();
    }

    // ==================== 파티 참가 ====================

    /**
     * 파티 참가
     */
    @Transactional
    public PartyDto.Response join(Long partyId) {
        User user = getCurrentUser();
        Party party = findPartyById(partyId);

        // 기존 참가 파티 탈퇴 처리
        leaveOtherParties(user, party);

        // 이미 참가한 경우
        if (party.getParticipants().contains(user)) {
            return toResponse(party);
        }

        // 정원 확인
        if (party.getParticipants().size() >= party.getCapacity()) {
            throw BadRequestException.partyFull();
        }

        // 타임테이블 등록
        addTimetableEntry(party);

        party.getParticipants().add(user);
        auditPartyAction(party.getServer().getId(), user.getId(), AuditAction.JOIN_PARTY, party);

        return toResponse(party);
    }

    // ==================== 파티 탈퇴 ====================

    /**
     * 파티 탈퇴
     */
    @Transactional
    public PartyDto.Response leaveParty(Long partyId) {
        User user = getCurrentUser();
        Party party = findPartyById(partyId);

        if (party.getParticipants() != null) {
            party.getParticipants().remove(user);
        }

        // 해당 서버에서 내 스케줄 삭제
        removeMyTimetableEntry(party.getServer(), user);

        auditPartyAction(party.getServer().getId(), user.getId(), AuditAction.LEAVE_PARTY, party);

        return toResponse(party);
    }

    // ==================== 파티 삭제 ====================

    /**
     * 파티 삭제 (생성자만 가능)
     */
    @Transactional
    public void deleteParty(Long partyId) {
        User me = getCurrentUser();
        Party party = findPartyById(partyId);

        validateIsPartyCreator(party, me);

        // 참가자들의 스케줄 삭제
        deleteParticipantsTimetableEntries(party);

        auditPartyAction(party.getServer().getId(), me.getId(), AuditAction.DELETE_PARTY, party);

        partyRepo.delete(party);
    }

    /**
     * 파티 삭제 (엔드포인트 래퍼)
     */
    @Transactional
    public void deletePartyEndpoint(Long partyId) {
        deleteParty(partyId);
    }

    // ==================== Private 헬퍼 메서드 ====================

    private Server findServerById(Long serverId) {
        return serverRepo.findById(serverId)
                .orElseThrow(NotFoundException::server);
    }

    private Party findPartyById(Long partyId) {
        return partyRepo.findById(partyId)
                .orElseThrow(NotFoundException::party);
    }

    private Party buildParty(PartyDto.CreateRequest req, User creator, Server server) {
        Party party = Party.builder()
                .server(server)
                .creator(creator)
                .slot(req.getSlot().truncatedTo(ChronoUnit.MINUTES))
                .capacity(Math.max(1, req.getCapacity()))
                .createdAt(LocalDateTime.now())
                .build();

        setPartyGame(party, req);
        return party;
    }

    private void setPartyGame(Party party, PartyDto.CreateRequest req) {
        if (req.getCustomGameId() != null) {
            CustomGame customGame = customGameRepo.findById(req.getCustomGameId())
                    .orElseThrow(() -> BadRequestException.invalidInput("유효하지 않은 커스텀 게임 ID입니다"));
            party.setCustomGame(customGame);
        } else if (req.getDefaultGameId() != null) {
            DefaultGame defaultGame = defaultGameRepo.findById(req.getDefaultGameId())
                    .orElseThrow(() -> BadRequestException.invalidInput("유효하지 않은 기본 게임 ID입니다"));
            party.setDefaultGame(defaultGame);
        } else {
            throw BadRequestException.invalidInput("게임 ID가 필요합니다");
        }
    }

    private void leaveOtherParties(User user, Party targetParty) {
        List<Party> myParties = partyRepo.findByParticipantsContaining(user);

        for (Party party : myParties) {
            if (!party.getId().equals(targetParty.getId())) {
                if (party.getParticipants() != null) {
                    party.getParticipants().remove(user);
                }
                removeMyTimetableEntry(party.getServer(), user);
            }
        }
    }

    private void addTimetableEntry(Party party) {
        TimetableDto.EntryRequest req = new TimetableDto.EntryRequest();
        req.setServerId(party.getServer().getId());
        req.setSlot(party.getSlot());

        if (party.getCustomGame() != null) {
            req.setCustomGameId(party.getCustomGame().getId());
        } else if (party.getDefaultGame() != null) {
            req.setDefaultGameId(party.getDefaultGame().getId());
        }

        timetableService.add(req);
    }

    private void removeMyTimetableEntry(Server server, User user) {
        timetableService.deleteByServerAndCurrentUser(server.getId());
    }

    private void deleteParticipantsTimetableEntries(Party party) {
        if (party.getParticipants() != null && !party.getParticipants().isEmpty()) {
            Server server = party.getServer();
            for (User participant : party.getParticipants()) {
                timetableEntryRepo.deleteAllByServerAndUser(server, participant);
            }
        }
    }

    private void validateIsPartyCreator(Party party, User user) {
        if (!party.getCreator().getId().equals(user.getId())) {
            throw ForbiddenException.accessDenied();
        }
    }

    // ==================== 알림 관련 ====================

    private void notifyPartyCreated(Server server, User creator, Party party) {
        List<User> targets = server.getMembers().stream()
                .filter(m -> !m.getId().equals(creator.getId()))
                .toList();

        String gameName = getGameName(party);
        String payload = buildPartyNotificationPayload(server, creator, gameName, party.getCapacity());

        notificationService.notifyMany(
                targets,
                NotificationType.PARTY,
                "파티 모집",
                payload,
                server.getId()
        );
    }

    private String buildPartyNotificationPayload(Server server, User creator, String gameName, int capacity) {
        return String.format(
                "{\"kind\":\"party\",\"serverId\":%d,\"serverName\":\"%s\",\"fromNickname\":\"%s\",\"gameName\":\"%s\",\"capacity\":%d}",
                server.getId(),
                escapeJson(server.getName()),
                escapeJson(creator.getNickname()),
                escapeJson(gameName),
                capacity
        );
    }

    // ==================== 감사 로그 ====================

    private void auditPartyAction(Long serverId, Long userId, String action, Party party) {
        try {
            String gameName = getGameName(party);
            String details = String.format("game=%s;slot=%s;capacity=%d",
                    escapeJson(gameName), party.getSlot(), party.getCapacity());
            auditService.log(serverId, userId, action, details);
        } catch (Exception ignored) {
            // 감사 로그 실패는 비즈니스 로직에 영향주지 않음
        }
    }

    // ==================== 유틸리티 ====================

    private String getGameName(Party party) {
        if (party.getCustomGame() != null) {
            return party.getCustomGame().getName();
        } else if (party.getDefaultGame() != null) {
            return party.getDefaultGame().getName();
        }
        return "미정";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ==================== DTO 변환 ====================

    private PartyDto.Response toResponse(Party party) {
        PartyDto.Response response = new PartyDto.Response();
        response.setId(party.getId());
        response.setServerId(party.getServer().getId());
        response.setCreator(party.getCreator().getNickname());
        response.setSlot(party.getSlot());
        response.setCapacity(party.getCapacity());

        setGameInfo(response, party);
        setParticipantInfo(response, party);
        setUserStatus(response, party);

        return response;
    }

    private void setGameInfo(PartyDto.Response response, Party party) {
        if (party.getCustomGame() != null) {
            response.setGameId(party.getCustomGame().getId());
            response.setGameName(party.getCustomGame().getName());
            response.setCustom(true);
        } else if (party.getDefaultGame() != null) {
            response.setGameId(party.getDefaultGame().getId());
            response.setGameName(party.getDefaultGame().getName());
            response.setCustom(false);
        } else {
            response.setGameId(null);
            response.setGameName("미정");
            response.setCustom(false);
        }
    }

    private void setParticipantInfo(PartyDto.Response response, Party party) {
        int participantCount = party.getParticipants() == null ? 0 : party.getParticipants().size();
        response.setParticipants(participantCount);
        response.setFull(participantCount >= party.getCapacity());

        Set<String> names = party.getParticipants() == null
                ? Set.of()
                : party.getParticipants().stream()
                        .map(User::getNickname)
                        .collect(Collectors.toSet());
        response.setParticipantNames(names);
    }

    private void setUserStatus(PartyDto.Response response, Party party) {
        try {
            User me = getCurrentUser();
            response.setJoined(party.getParticipants() != null && party.getParticipants().contains(me));
            response.setOwner(party.getCreator() != null && party.getCreator().getId().equals(me.getId()));
        } catch (Exception ignored) {
            // 인증되지 않은 경우 기본값 유지
        }
    }
}
