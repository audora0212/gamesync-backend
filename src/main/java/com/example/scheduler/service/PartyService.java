package com.example.scheduler.service;

import com.example.scheduler.domain.*;
import com.example.scheduler.dto.PartyDto;
import com.example.scheduler.dto.TimetableDto;
import com.example.scheduler.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartyService {
    private final PartyRepository partyRepo;
    private final ServerRepository serverRepo;
    private final UserRepository userRepo;
    private final DefaultGameRepository defaultGameRepo;
    private final CustomGameRepository customGameRepo;
    private final TimetableService timetableService;
    private final NotificationService notificationService;
    private final TimetableEntryRepository timetableEntryRepository;
    private final AuditService auditService;

    private User currentUser() {
        return userRepo.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public PartyDto.Response create(PartyDto.CreateRequest req) {
        User user = currentUser();
        Server server = serverRepo.findById(req.getServerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Party p = Party.builder()
                .server(server)
                .creator(user)
                .slot(req.getSlot().truncatedTo(ChronoUnit.MINUTES))
                .capacity(Math.max(1, req.getCapacity()))
                .createdAt(LocalDateTime.now())
                .build();

        if (req.getCustomGameId() != null) {
            CustomGame cg = customGameRepo.findById(req.getCustomGameId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid customGameId"));
            p.setCustomGame(cg);
        } else if (req.getDefaultGameId() != null) {
            DefaultGame dg = defaultGameRepo.findById(req.getDefaultGameId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid defaultGameId"));
            p.setDefaultGame(dg);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }

        // 우선 파티를 저장
        Party saved = partyRepo.save(p);
        try {
            String gameName = (saved.getCustomGame() != null) ? saved.getCustomGame().getName() : saved.getDefaultGame().getName();
            String details = String.format("game=%s;slot=%s;capacity=%d", safe(gameName), saved.getSlot(), saved.getCapacity());
            auditService.log(server.getId(), user.getId(), "PARTY_CREATE", details);
        } catch (Exception ignored) {}

        // 생성자의 타임테이블 먼저 등록 (파티 참가 이전에 수행하여 파티 참가중 가드에 걸리지 않도록 함)
        TimetableDto.EntryRequest tReq = new TimetableDto.EntryRequest();
        tReq.setServerId(saved.getServer().getId());
        tReq.setSlot(saved.getSlot());
        if (saved.getCustomGame() != null) {
            tReq.setCustomGameId(saved.getCustomGame().getId());
        } else if (saved.getDefaultGame() != null) {
            tReq.setDefaultGameId(saved.getDefaultGame().getId());
        }
        timetableService.add(tReq);

        // 생성자는 자동 참가
        saved.getParticipants().add(user);
        try {
            String gameName = (saved.getCustomGame() != null) ? saved.getCustomGame().getName() : saved.getDefaultGame().getName();
            String details = String.format("game=%s;slot=%s", safe(gameName), saved.getSlot());
            auditService.log(server.getId(), user.getId(), "PARTY_JOIN", details);
        } catch (Exception ignored) {}

        // 서버 모든 멤버에게 파티 모집 알림 (소유자 제외 가능) - 팬아웃을 notifyMany로 집계
        java.util.List<User> targets = new java.util.ArrayList<>();
        for (User m : server.getMembers()) {
            if (m.getId().equals(user.getId())) continue;
            targets.add(m);
        }
        String gameName = (saved.getCustomGame() != null) ? saved.getCustomGame().getName() : saved.getDefaultGame().getName();
        String title = "파티 모집";
        String payload = String.format(
                "{\"kind\":\"party\",\"serverId\":%d,\"serverName\":\"%s\",\"fromNickname\":\"%s\",\"gameName\":\"%s\",\"capacity\":%d}",
                server.getId(),
                safe(server.getName()),
                safe(user.getNickname()),
                safe(gameName),
                saved.getCapacity()
        );
        notificationService.notifyMany(targets, com.example.scheduler.domain.NotificationType.PARTY, title, payload, server.getId());

        return toResp(saved);
    }

    public List<PartyDto.Response> list(Long serverId) {
        Server server = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return partyRepo.findByServerOrderBySlotAsc(server).stream()
                .map(this::toResp)
                .toList();
    }

    @Transactional
    public PartyDto.Response join(Long partyId) {
        User user = currentUser();
        Party party = partyRepo.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 하나의 유저는 하나의 파티만 참가 가능: 기존 참가 파티 탈퇴
        List<Party> myParties = partyRepo.findByParticipantsContaining(user);
        for (Party p : myParties) {
            if (!p.getId().equals(party.getId())) {
                if (p.getParticipants() != null) p.getParticipants().remove(user);
                // 기존 서버에서 내 스케줄 삭제
                removeMyTimetableEntry(p.getServer(), user);
            }
        }

        if (party.getParticipants().contains(user)) {
            return toResp(party);
        }
        if (party.getParticipants().size() >= party.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party is full");
        }

        // 타임테이블에도 등록
        TimetableDto.EntryRequest req = new TimetableDto.EntryRequest();
        req.setServerId(party.getServer().getId());
        req.setSlot(party.getSlot());
        if (party.getCustomGame() != null) {
            req.setCustomGameId(party.getCustomGame().getId());
        } else if (party.getDefaultGame() != null) {
            req.setDefaultGameId(party.getDefaultGame().getId());
        }
        timetableService.add(req);

        party.getParticipants().add(user);
        try {
            String gameName = (party.getCustomGame() != null) ? party.getCustomGame().getName() : party.getDefaultGame().getName();
            String details = String.format("game=%s;slot=%s", safe(gameName), party.getSlot());
            auditService.log(party.getServer().getId(), user.getId(), "PARTY_JOIN", details);
        } catch (Exception ignored) {}
        return toResp(party);
    }

    @Transactional
    public PartyDto.Response leaveParty(Long partyId) {
        User user = currentUser();
        Party party = partyRepo.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (party.getParticipants() != null) {
            party.getParticipants().remove(user);
        }

        // 해당 서버에서 내 스케줄 삭제
        // 한 서버당 하나만 유지되는 정책이므로 서버-유저 조합 모두 삭제
        removeMyTimetableEntry(party.getServer(), user);

        try {
            String gameName = (party.getCustomGame() != null) ? party.getCustomGame().getName() : party.getDefaultGame().getName();
            String details = String.format("game=%s;slot=%s", safe(gameName), party.getSlot());
            auditService.log(party.getServer().getId(), user.getId(), "PARTY_LEAVE", details);
        } catch (Exception ignored) {}
        return toResp(party);
    }

    private void removeMyTimetableEntry(Server server, User user) {
        // TimetableService에 공개 메서드가 없으므로 repository를 직접 사용하도록 서비스에 위임하는 메서드를 추가하는 것이 이상적이나,
        // 현재 의존성 구성을 유지하기 위해 TimetableService에 helper를 추가하는 대신, repository를 PartyService에 주입하는 것은 구조상 좋지 않습니다.
        // 간단히 TimetableService에 메서드가 있다고 가정하고 위임합니다. (아래에서 실제 메서드 추가)
        timetableService.deleteByServerAndCurrentUser(server.getId());
    }

    private PartyDto.Response toResp(Party party) {
        PartyDto.Response r = new PartyDto.Response();
        r.setId(party.getId());
        r.setServerId(party.getServer().getId());
        r.setCreator(party.getCreator().getNickname());
        r.setSlot(party.getSlot());
        if (party.getCustomGame() != null) {
            r.setGameId(party.getCustomGame().getId());
            r.setGameName(party.getCustomGame().getName());
            r.setCustom(true);
        } else if (party.getDefaultGame() != null) {
            r.setGameId(party.getDefaultGame().getId());
            r.setGameName(party.getDefaultGame().getName());
            r.setCustom(false);
        } else {
            r.setGameId(null);
            r.setGameName("미정");
            r.setCustom(false);
        }
        r.setCapacity(party.getCapacity());
        int participants = party.getParticipants() == null ? 0 : party.getParticipants().size();
        r.setParticipants(participants);
        r.setFull(participants >= party.getCapacity());
        Set<String> names = party.getParticipants() == null ? Set.of() : party.getParticipants().stream()
                .map(User::getNickname)
                .collect(Collectors.toSet());
        r.setParticipantNames(names);
        try {
            User me = currentUser();
            r.setJoined(party.getParticipants() != null && party.getParticipants().contains(me));
            r.setOwner(party.getCreator() != null && party.getCreator().getId().equals(me.getId()));
        } catch (Exception ignore) { }
        return r;
    }

    @Transactional
    public void deleteParty(Long partyId) {
        User me = currentUser();
        Party party = partyRepo.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!party.getCreator().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // 파티 삭제 시 참가자들의 해당 서버 스케줄 제거
        if (party.getParticipants() != null && !party.getParticipants().isEmpty()) {
            Server server = party.getServer();
            for (User u : party.getParticipants()) {
                timetableEntryRepository.deleteAllByServerAndUser(server, u);
            }
        }
        try {
            String gameName = (party.getCustomGame() != null) ? party.getCustomGame().getName() : party.getDefaultGame().getName();
            String details = String.format("game=%s;slot=%s", safe(gameName), party.getSlot());
            auditService.log(party.getServer().getId(), me.getId(), "PARTY_DELETE", details);
        } catch (Exception ignored) {}
        partyRepo.delete(party);
    }

    // Wrapper to satisfy certain call sites if needed
    @Transactional
    public void deletePartyEndpoint(Long partyId) {
        deleteParty(partyId);
    }
    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
