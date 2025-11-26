package com.example.scheduler.controller;

import com.example.scheduler.dto.TimetableDto;
import com.example.scheduler.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/timetable")
@RequiredArgsConstructor
@Tag(name = "Timetable", description = "타임테이블(스케줄) 관리 API")
public class TimetableController {

    private final TimetableService timetableService;

    @Operation(summary = "스케줄 등록", description = "게임 플레이 예정 시간을 등록합니다")
    @PostMapping
    public ResponseEntity<TimetableDto.EntryResponse> add(
            @PathVariable Long serverId,
            @Valid @RequestBody TimetableDto.EntryRequest req) {
        req.setServerId(serverId);
        return ResponseEntity.ok(timetableService.add(req));
    }

    @Operation(summary = "스케줄 목록", description = "서버의 전체 스케줄을 조회합니다")
    @GetMapping
    public ResponseEntity<List<TimetableDto.EntryResponse>> list(
            @PathVariable Long serverId,
            @RequestParam(required = false) String game,
            @RequestParam(defaultValue = "false") boolean sortByGame) {
        return ResponseEntity.ok(timetableService.list(serverId, game, sortByGame));
    }

    @Operation(summary = "스케줄 통계", description = "서버의 스케줄 통계를 조회합니다")
    @GetMapping("/stats")
    public ResponseEntity<TimetableDto.StatsResponse> stats(@PathVariable Long serverId) {
        return ResponseEntity.ok(timetableService.stats(serverId));
    }

    @Operation(summary = "내 스케줄 삭제", description = "현재 사용자의 스케줄을 삭제합니다")
    @DeleteMapping
    public ResponseEntity<Void> deleteMyEntry(@PathVariable Long serverId) {
        timetableService.deleteByServerAndCurrentUser(serverId);
        return ResponseEntity.noContent().build();
    }
}