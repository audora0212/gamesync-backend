// controller/TimetableController.java
package com.example.scheduler.controller;

import com.example.scheduler.dto.TimetableDto;
import com.example.scheduler.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/timetable")
@RequiredArgsConstructor
public class TimetableController {
    private final TimetableService timetableService;

    @PostMapping
    public ResponseEntity<TimetableDto.EntryResponse> add(
            @PathVariable Long serverId,
            @RequestBody TimetableDto.EntryRequest req) {
        req.setServerId(serverId);
        return ResponseEntity.ok(timetableService.add(req));
    }

    @GetMapping
    public ResponseEntity<List<TimetableDto.EntryResponse>> list(
            @PathVariable Long serverId,
            @RequestParam(required = false) String game,
            @RequestParam(defaultValue = "false") boolean sortByGame) {
        return ResponseEntity.ok(timetableService.list(serverId, game, sortByGame));
    }

    @GetMapping("/stats")
    public ResponseEntity<TimetableDto.StatsResponse> stats(@PathVariable Long serverId) {
        return ResponseEntity.ok(timetableService.stats(serverId));
    }
}