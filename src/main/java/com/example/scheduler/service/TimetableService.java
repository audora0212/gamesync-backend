package com.example.scheduler.service;

import com.example.scheduler.domain.*;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {
    private final TimetableEntryRepository entryRepo;
    private final ServerRepository serverRepo;
    private final UserRepository userRepo;
    private final DefaultGameRepository defaultGameRepo;
    private final CustomGameRepository customGameRepo;
    private final NotificationService notificationService;
    private final FriendshipRepository friendshipRepository;
    private final PartyRepository partyRepository;
    private final AuditService auditService;

    @Transactional
    public TimetableDto.EntryResponse add(TimetableDto.EntryRequest req) {
        User user = userRepo.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Server srv = serverRepo.findById(req.getServerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 파티에 참가 중이면 스케줄 신규 등록 금지
        if (partyRepository.existsByServerAndParticipantsContaining(srv, user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 파티에 참가 중입니다. 파티를 떠난 후 예약해 주세요.");
        }

        entryRepo.findByServerAndUser(srv, user)
                .ifPresent(entryRepo::delete);

        TimetableEntry e = TimetableEntry.builder()
                .server(srv)
                .user(user)
                .slot(req.getSlot().truncatedTo(ChronoUnit.MINUTES))
                .build();

        if (req.getCustomGameId() != null) {
            CustomGame cg = customGameRepo.findById(req.getCustomGameId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid customGameId"));
            e.setCustomGame(cg);
        } else if (req.getDefaultGameId() != null) {
            DefaultGame dg = defaultGameRepo.findById(req.getDefaultGameId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid defaultGameId"));
            e.setDefaultGame(dg);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game ID is required");
        }

        entryRepo.save(e);
        // 감사 로그: 스케줄 등록 기록 (집계용: game,slot 포함)
        try {
            String details = String.format("game=%s;slot=%s", safeGameName(e), e.getSlot().toString());
            auditService.log(srv.getId(), user.getId(), "TIMETABLE_REGISTER", details);
        } catch (Exception ignored) {}
        // 알림: 같은 서버의 내 친구들에게 통지 (JSON payload에 serverId 포함) - 집계 전송
        notifyFriendsInServer(user, srv, e);
        return toResp(e);
    }

    public List<TimetableDto.EntryResponse> list(
            Long serverId, String gameName, boolean sortByGame
    ) {
        Server srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 1) 서버 전체 엔트리 조회
        List<TimetableEntry> list = entryRepo.findByServerOrderBySlot(srv);

        // 2) 이름 기준 필터링 (gameName 파라미터 존재 시)
        if (gameName != null) {
            list = list.stream()
                    .filter(e -> {
                        String name = (e.getCustomGame() != null)
                                ? e.getCustomGame().getName()
                                : e.getDefaultGame().getName();
                        return name.equals(gameName);
                    })
                    .collect(Collectors.toList());
        }

        // 3) 게임명 기준 정렬
        if (sortByGame) {
            list.sort(Comparator.comparing(e -> {
                return (e.getCustomGame() != null)
                        ? e.getCustomGame().getName()
                        : e.getDefaultGame().getName();
            }));
        }

        return list.stream().map(this::toResp).collect(Collectors.toList());
    }

    public TimetableDto.StatsResponse stats(Long serverId) {
        Server srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<TimetableEntry> list = entryRepo.findByServerOrderBySlot(srv);

        // 최다 플레이 게임
        String topGame = list.stream()
                .collect(Collectors.groupingBy(e -> {
                    return (e.getCustomGame() != null)
                            ? e.getCustomGame().getName()
                            : e.getDefaultGame().getName();
                }, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get().getKey();

        // 슬롯별 카운트
        Map<LocalDateTime, Long> slotCounts = list.stream()
                .collect(Collectors.groupingBy(TimetableEntry::getSlot, Collectors.counting()));

        // 피크 슬롯과 평균 슬롯
        LocalDateTime peakSlot = slotCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get().getKey();

        double avgMinute = list.stream()
                .mapToLong(e -> e.getSlot().getHour() * 60 + e.getSlot().getMinute())
                .average().orElse(0);

        LocalDateTime avgSlot = LocalDateTime.now()
                .withHour((int)avgMinute / 60)
                .withMinute((int)avgMinute % 60)
                .truncatedTo(ChronoUnit.MINUTES);

        int peakCount = slotCounts.get(peakSlot).intValue();
        return new TimetableDto.StatsResponse(topGame, avgSlot, peakSlot, peakCount);
    }

    @Transactional
    public void deleteByServerAndCurrentUser(Long serverId) {
        User user = userRepo.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Server srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            // 감사 로그: 파티 이동/탈퇴 등으로 내 타임테이블 삭제
            var entries = entryRepo.findByServerAndUser(srv, user);
            entries.ifPresent(e -> {
                String details = String.format("reason=USER_ACTION;game=%s;slot=%s",
                        safeGameName(e), e.getSlot());
                auditService.log(srv.getId(), user.getId(), "TIMETABLE_DELETE", details);
            });
        } catch (Exception ignored) {}
        entryRepo.deleteAllByServerAndUser(srv, user);
    }

    @Transactional
    public TimetableDto.EntryResponse update(Long serverId, LocalDateTime newSlot, Long defaultGameId, Long customGameId) {
        User user = userRepo.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Server srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TimetableEntry e = entryRepo.findByServerAndUser(srv, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        LocalDateTime oldSlot = e.getSlot();
        String oldGame = safeGameName(e);
        if (customGameId != null) {
            CustomGame cg = customGameRepo.findById(customGameId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid customGameId"));
            e.setCustomGame(cg);
            e.setDefaultGame(null);
        } else if (defaultGameId != null) {
            DefaultGame dg = defaultGameRepo.findById(defaultGameId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid defaultGameId"));
            e.setDefaultGame(dg);
            e.setCustomGame(null);
        }
        if (newSlot != null) e.setSlot(newSlot.truncatedTo(ChronoUnit.MINUTES));
        entryRepo.save(e);
        try {
            String details = String.format("fromGame=%s;fromSlot=%s;toGame=%s;toSlot=%s",
                    oldGame, oldSlot, safeGameName(e), e.getSlot());
            auditService.log(srv.getId(), user.getId(), "TIMETABLE_UPDATE", details);
        } catch (Exception ignored) {}
        return toResp(e);
    }

    private TimetableDto.EntryResponse toResp(TimetableEntry e) {
        TimetableDto.EntryResponse r = new TimetableDto.EntryResponse();
        r.setId(e.getId());
        r.setUser(e.getUser().getNickname());
        r.setSlot(e.getSlot());
        if (e.getCustomGame() != null) {
            r.setGameId(e.getCustomGame().getId());
            r.setGameName(e.getCustomGame().getName());
            r.setCustom(true);
        } else if (e.getDefaultGame() != null) {
            r.setGameId(e.getDefaultGame().getId());
            r.setGameName(e.getDefaultGame().getName());
            r.setCustom(false);
        } else {
            r.setGameId(null);
            r.setGameName("미정");
            r.setCustom(false);
        }
        return r;
    }

    private void notifyFriendsInServer(User actor, Server server, TimetableEntry entry) {
        // actor의 친구 집합(양방향 저장 고려) 수집
        var friendsA = friendshipRepository.findByUser(actor).stream().map(f -> f.getFriend()).toList();
        var friendsB = friendshipRepository.findByFriend(actor).stream().map(f -> f.getUser()).toList();
        java.util.Set<Long> friendIds = new java.util.HashSet<>();
        friendsA.forEach(u -> friendIds.add(u.getId()));
        friendsB.forEach(u -> friendIds.add(u.getId()));

        // 같은 서버 멤버 중 친구에게만 알림을 모아서 한 번에 전송
        java.util.List<User> targets = new java.util.ArrayList<>();
        for (User m : server.getMembers()) {
            if (!m.getId().equals(actor.getId()) && friendIds.contains(m.getId())) {
                targets.add(m);
            }
        }
        if (!targets.isEmpty()) {
            String gameName = (entry.getCustomGame() != null)
                    ? entry.getCustomGame().getName()
                    : entry.getDefaultGame().getName();
            String payload = String.format(
                    "{\"kind\":\"timetable\",\"serverId\":%d,\"serverName\":\"%s\",\"fromNickname\":\"%s\",\"gameName\":\"%s\"}",
                    server.getId(),
                    safe(server.getName()),
                    safe(actor.getNickname()),
                    safe(gameName)
            );
            notificationService.notifyMany(targets, com.example.scheduler.domain.NotificationType.TIMETABLE, "친구의 스케줄 등록", payload, server.getId());
        }
    }

    private String safeGameName(TimetableEntry entry) {
        if (entry.getCustomGame() != null) return entry.getCustomGame().getName();
        if (entry.getDefaultGame() != null) return entry.getDefaultGame().getName();
        return "";
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
