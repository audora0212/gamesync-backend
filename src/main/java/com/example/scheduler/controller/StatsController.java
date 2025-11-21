package com.example.scheduler.controller;

import com.example.scheduler.dto.StatsDto;
import com.example.scheduler.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servers/{serverId}/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<StatsDto.AggregatedResponse> getAggregated(
            @PathVariable Long serverId,
            @RequestParam(defaultValue = "weekly") String range
    ) {
        return ResponseEntity.ok(statsService.aggregate(serverId, range));
    }

    @GetMapping("/today")
    public ResponseEntity<StatsDto.TodayStatsResponse> getToday(
            @PathVariable Long serverId
    ) {
        return ResponseEntity.ok(statsService.today(serverId));
    }

    @GetMapping("/weekly")
    public ResponseEntity<StatsDto.WeeklyStatsResponse> getWeekly(
            @PathVariable Long serverId
    ) {
        return ResponseEntity.ok(statsService.weekly(serverId));
    }
}


