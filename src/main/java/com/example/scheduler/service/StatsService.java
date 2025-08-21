package com.example.scheduler.service;

import com.example.scheduler.domain.TimetableEntry;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.StatsDto;
import com.example.scheduler.repository.AuditLogRepository;
import com.example.scheduler.repository.ServerRepository;
import com.example.scheduler.repository.TimetableEntryRepository;
import com.example.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final ServerRepository serverRepo;
    private final TimetableEntryRepository entryRepo;
    private final AuditLogRepository auditRepo;
    private final UserRepository userRepo;

    public StatsDto.AggregatedResponse aggregate(Long serverId, String range) {
        var srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 월간 제거: range는 weekly만 허용
        if (!"weekly".equalsIgnoreCase(range)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range must be weekly");
        }
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays((today.getDayOfWeek().getValue() + 6) % 7);
        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = monday.plusDays(7).atTime(LocalTime.MAX);

        var logs = auditRepo.findByServerIdAndActionAndOccurredAtBetween(serverId, "TIMETABLE_REGISTER", start, end);

        List<String> gameNames;
        List<LocalDateTime> slots;
        if (!logs.isEmpty()) {
            gameNames = new ArrayList<>();
            slots = new ArrayList<>();
            for (var l : logs) {
                String d = l.getDetails();
                if (d == null || d.isBlank()) continue;
                // format: game=<name>;slot=2025-08-21T20:10
                String[] parts = d.split(";");
                String game = null; String slot = null;
                for (String p : parts) {
                    int idx = p.indexOf('=');
                    if (idx <= 0) continue;
                    String k = p.substring(0, idx).trim();
                    String v = p.substring(idx + 1).trim();
                    if ("game".equals(k)) game = v;
                    else if ("slot".equals(k)) slot = v;
                }
                if (game != null && !game.isBlank()) gameNames.add(game);
                if (slot != null) {
                    try { slots.add(LocalDateTime.parse(slot)); } catch (Exception ignored) {}
                }
            }
            // 보조 데이터가 완전히 비었을 경우에만 현재 엔트리 사용
            if (slots.isEmpty()) {
                List<TimetableEntry> current = entryRepo.findByServerOrderBySlot(srv);
                slots = current.stream().map(TimetableEntry::getSlot).toList();
            }
        } else {
            List<TimetableEntry> current = entryRepo.findByServerOrderBySlot(srv);
            gameNames = current.stream().map(e -> e.getCustomGame() != null ? e.getCustomGame().getName() : e.getDefaultGame().getName()).toList();
            slots = current.stream().map(TimetableEntry::getSlot).toList();
        }

        Map<String, Long> gameCounts = gameNames.stream()
                .collect(Collectors.groupingBy(n -> n, Collectors.counting()));

        List<StatsDto.NameCount> topGames = gameCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new StatsDto.NameCount(e.getKey(), e.getValue()))
                .toList();

        long[] hourCountsArr = new long[24];
        for (LocalDateTime s : slots) {
            hourCountsArr[s.getHour()]++;
        }
        List<StatsDto.HourCount> hourCounts = new ArrayList<>();
        int topHour = 0; long topHourCount = 0;
        for (int h = 0; h < 24; h++) {
            long c = hourCountsArr[h];
            hourCounts.add(new StatsDto.HourCount(h, c));
            if (c > topHourCount) { topHourCount = c; topHour = h; }
        }

        String topGame = topGames.isEmpty() ? null : topGames.get(0).getName();

        // 주간 데이터 수집중 여부: 해당 서버의 최초 TIMETABLE_REGISTER 로그 일자가 월요일(start) 이후이면 이번 주 데이터 일부만 존재
        boolean collecting = false;
        try {
            var first = auditRepo.findFirstByServerIdAndActionOrderByOccurredAtAsc(serverId, "TIMETABLE_REGISTER");
            if (first == null || first.getOccurredAt().isAfter(start)) {
                collecting = true;
            }
        } catch (Exception ignored) {}

        return new StatsDto.AggregatedResponse(
                "weekly", start, end, topGame, topHour, topHourCount, topGames, hourCounts, collecting
        );
    }

    public StatsDto.TodayStatsResponse today(Long serverId) {
        serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1).minusNanos(1);

        // 오늘 등록된 슬롯 수집: 현재 구조에서는 엔트리만으로는 과거가 없으므로 AuditLog 사용 권장
        var logs = auditRepo.findByServerIdAndActionAndOccurredAtBetween(serverId, "TIMETABLE_REGISTER", start, end);
        List<String> gameNames = new ArrayList<>();
        long[] hourly = new long[24];
        List<Integer> minutes = new ArrayList<>();
        for (var l : logs) {
            String d = l.getDetails();
            String game = null; String slotS = null;
            if (d != null) {
                for (String p : d.split(";")) {
                    int idx = p.indexOf('=');
                    if (idx <= 0) continue;
                    String k = p.substring(0, idx).trim();
                    String v = p.substring(idx + 1).trim();
                    if ("game".equals(k)) game = v;
                    else if ("slot".equals(k)) slotS = v;
                }
            }
            if (game != null) gameNames.add(game);
            LocalDateTime s = l.getOccurredAt();
            if (slotS != null) { try { s = LocalDateTime.parse(slotS); } catch (Exception ignored) {} }
            if (s != null) {
                hourly[s.getHour()]++;
                minutes.add(s.getHour()*60 + s.getMinute());
            }
        }

        // topGame
        String topGame = gameNames.stream()
                .collect(Collectors.groupingBy(n -> n, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);

        // avgMinute
        int avgMinute = minutes.isEmpty() ? 0 : (int)Math.round(minutes.stream().mapToInt(i->i).average().orElse(0));

        // peak
        int peakHour = 0; long peakCount = 0;
        List<StatsDto.HourCount> hourlyCounts = new ArrayList<>();
        for (int h=0; h<24; h++) { hourlyCounts.add(new StatsDto.HourCount(h, hourly[h])); if (hourly[h]>peakCount){ peakCount=hourly[h]; peakHour=h; } }

        return new StatsDto.TodayStatsResponse(topGame, avgMinute, peakHour, peakCount, hourlyCounts);
    }

    public StatsDto.WeeklyStatsResponse weekly(Long serverId) {
        var srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDate monday = LocalDate.now().minusDays((LocalDate.now().getDayOfWeek().getValue()+6)%7);
        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = start.plusDays(7).minusNanos(1);

        var logs = auditRepo.findByServerIdAndActionAndOccurredAtBetween(serverId, "TIMETABLE_REGISTER", start, end);

        // 1) 가장 자주 접속한 유저 (활동 일수 기준 최대 7)
        Map<Long, java.util.Set<Integer>> userToDows = new HashMap<>();
        for (var l: logs) {
            int dow = l.getOccurredAt().getDayOfWeek().getValue();
            userToDows.computeIfAbsent(l.getUserId(), k->new java.util.HashSet<>()).add(dow);
        }
        List<StatsDto.UserCount> topUsers = userToDows.entrySet().stream()
                .map(e -> new StatsDto.UserCount(e.getKey(), null, e.getValue().size()))
                .sorted((a,b)-> Long.compare(b.getCount(), a.getCount()))
                .limit(3).collect(Collectors.toList());
        // 닉네임 채우기
        var ids = topUsers.stream().map(StatsDto.UserCount::getUserId).toList();
        var userMap = userRepo.findAllById(ids).stream().collect(Collectors.toMap(User::getId, u->u));
        topUsers.forEach(u -> { var user = userMap.get(u.getUserId()); if (user!=null) u.setNickname(user.getNickname()); });

        // 2) 요일별 평균 접속 시간대
        Map<Integer, List<Integer>> dowMinutes = new HashMap<>();
        for (var l: logs) {
            LocalDateTime s = l.getOccurredAt();
            String d = l.getDetails();
            if (d != null) {
                for (String p : d.split(";")) {
                    int idx = p.indexOf('=');
                    if (idx <= 0) continue;
                    String k = p.substring(0, idx).trim();
                    String v = p.substring(idx + 1).trim();
                    if ("slot".equals(k)) { try { s = LocalDateTime.parse(v);} catch (Exception ignored) {} }
                }
            }
            int dow = s.getDayOfWeek().getValue();
            dowMinutes.computeIfAbsent(dow, k->new ArrayList<>()).add(s.getHour()*60 + s.getMinute());
        }
        List<StatsDto.DayAvg> dowAvg = new ArrayList<>();
        for (int d=1; d<=7; d++) {
            List<Integer> list = dowMinutes.getOrDefault(d, java.util.Collections.emptyList());
            int avg = list.isEmpty()? 0 : (int)Math.round(list.stream().mapToInt(i->i).average().orElse(0));
            dowAvg.add(new StatsDto.DayAvg(d, avg, list.size()));
        }

        // 3) 요일별 플레이 된 게임(막대: 게임별 카운트)
        Map<Integer, Map<String, Long>> dowGameCounts = new HashMap<>();
        for (var l: logs) {
            String game = null; String d = l.getDetails();
            if (d != null) {
                for (String p : d.split(";")) {
                    int idx = p.indexOf('=');
                    if (idx <= 0) continue;
                    String k = p.substring(0, idx).trim();
                    String v = p.substring(idx + 1).trim();
                    if ("game".equals(k)) game = v;
                }
            }
            if (game == null) continue;
            int dow = l.getOccurredAt().getDayOfWeek().getValue();
            dowGameCounts.computeIfAbsent(dow, k-> new HashMap<>())
                    .merge(game, 1L, Long::sum);
        }
        List<StatsDto.DayGames> dowGames = new ArrayList<>();
        for (int d=1; d<=7; d++) {
            Map<String, Long> m = dowGameCounts.getOrDefault(d, java.util.Collections.emptyMap());
            List<StatsDto.NameCount> items = m.entrySet().stream()
                    .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                    .map(e -> new StatsDto.NameCount(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            dowGames.add(new StatsDto.DayGames(d, items));
        }

        return new StatsDto.WeeklyStatsResponse(topUsers, dowAvg, dowGames);
    }
}


