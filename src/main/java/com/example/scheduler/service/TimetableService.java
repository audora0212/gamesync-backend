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

    @Transactional
    public TimetableDto.EntryResponse add(TimetableDto.EntryRequest req) {
        User user = userRepo.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Server srv = serverRepo.findById(req.getServerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

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

    private TimetableDto.EntryResponse toResp(TimetableEntry e) {
        TimetableDto.EntryResponse r = new TimetableDto.EntryResponse();
        r.setId(e.getId());
        r.setUser(e.getUser().getNickname());
        r.setSlot(e.getSlot());
        if (e.getCustomGame() != null) {
            r.setGameId(e.getCustomGame().getId());
            r.setGameName(e.getCustomGame().getName());
            r.setCustom(true);
        } else {
            r.setGameId(e.getDefaultGame().getId());
            r.setGameName(e.getDefaultGame().getName());
            r.setCustom(false);
        }
        return r;
    }
}
