package com.example.scheduler.controller;

import com.example.scheduler.dto.GameDto;
import com.example.scheduler.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Game", description = "게임 관리 API")
public class GameController {

    private final GameService gameService;

    @Operation(summary = "기본 게임 목록", description = "시스템에 등록된 기본 게임 목록을 조회합니다")
    @GetMapping("/games/default")
    public ResponseEntity<GameDto.DefaultGameListResponse> getDefaultGames() {
        List<GameDto.DefaultGameResponse> list = gameService.listAllDefault();
        return ResponseEntity.ok(new GameDto.DefaultGameListResponse(list));
    }

    @Operation(summary = "커스텀 게임 목록", description = "서버의 커스텀 게임 목록을 조회합니다")
    @GetMapping("/servers/{serverId}/custom-games")
    public ResponseEntity<GameDto.CustomGameListResponse> getCustomGames(@PathVariable Long serverId) {
        List<GameDto.CustomGameResponse> list = gameService.listCustomByServer(serverId);
        return ResponseEntity.ok(new GameDto.CustomGameListResponse(list));
    }

    @Operation(summary = "커스텀 게임 추가", description = "서버에 커스텀 게임을 추가합니다")
    @PostMapping("/servers/{serverId}/custom-games")
    public ResponseEntity<GameDto.CustomGameResponse> addCustomGame(
            @PathVariable Long serverId,
            @RequestBody GameDto.CustomGameRequest req) {
        return ResponseEntity.ok(gameService.addCustomGame(serverId, req));
    }

    @Operation(summary = "커스텀 게임 삭제", description = "서버의 커스텀 게임을 삭제합니다")
    @DeleteMapping("/servers/{serverId}/custom-games/{gameId}")
    public ResponseEntity<Void> deleteCustomGame(
            @PathVariable Long serverId,
            @PathVariable Long gameId) {
        gameService.deleteCustomGame(serverId, gameId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게임 예약 사용자", description = "특정 게임을 예약한 사용자 목록을 조회합니다")
    @GetMapping("/servers/{serverId}/custom-games/{gameId}/scheduled-users")
    public ResponseEntity<GameDto.ScheduledUserListResponse> getScheduledUsers(
            @PathVariable Long serverId,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.listScheduledUsers(serverId, gameId));
    }
}
