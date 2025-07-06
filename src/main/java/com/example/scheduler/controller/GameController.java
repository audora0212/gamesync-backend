// controller/GameController.java
package com.example.scheduler.controller;

import com.example.scheduler.dto.GameDto;
import com.example.scheduler.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /* ---------- 기본 / 커스텀 게임 조회 ---------- */

    @GetMapping("/games/default")
    public ResponseEntity<GameDto.DefaultGameListResponse> getDefaultGames() {
        List<GameDto.DefaultGameResponse> list = gameService.listAllDefault();
        return ResponseEntity.ok(new GameDto.DefaultGameListResponse(list));
    }

    @GetMapping("/servers/{serverId}/custom-games")
    public ResponseEntity<GameDto.CustomGameListResponse> getCustomGames(@PathVariable Long serverId) {
        List<GameDto.CustomGameResponse> list = gameService.listCustomByServer(serverId);
        return ResponseEntity.ok(new GameDto.CustomGameListResponse(list));
    }

    /* ---------- 커스텀 게임 추가 / 삭제 ---------- */

    @PostMapping("/servers/{serverId}/custom-games")
    public ResponseEntity<GameDto.CustomGameResponse> addCustomGame(
            @PathVariable Long serverId,
            @RequestBody GameDto.CustomGameRequest req) {
        return ResponseEntity.ok(gameService.addCustomGame(serverId, req));
    }

    @DeleteMapping("/servers/{serverId}/custom-games/{gameId}")   // ⭐ 커스텀 게임 삭제
    public ResponseEntity<Void> deleteCustomGame(
            @PathVariable Long serverId,
            @PathVariable Long gameId) {
        gameService.deleteCustomGame(serverId, gameId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/servers/{serverId}/custom-games/{gameId}/scheduled-users")
    public ResponseEntity<GameDto.ScheduledUserListResponse> getScheduledUsers(
            @PathVariable Long serverId,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.listScheduledUsers(serverId, gameId));
    }

}
