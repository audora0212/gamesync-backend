// service/GameService.java
package com.example.scheduler.service;

import com.example.scheduler.domain.CustomGame;
import com.example.scheduler.domain.DefaultGame;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.GameDto;
import com.example.scheduler.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final DefaultGameRepository defaultGameRepo;
    private final CustomGameRepository customGameRepo;
    private final ServerRepository serverRepo;
    private final UserRepository userRepo;
    private final TimetableEntryRepository entryRepo;

    /* ---------- 기본 / 커스텀 게임 조회 ---------- */

    public List<GameDto.DefaultGameResponse> listAllDefault() {
        return defaultGameRepo.findAll().stream()
                .map(dg -> new GameDto.DefaultGameResponse(dg.getId(), dg.getName()))
                .collect(Collectors.toList());
    }

    public List<GameDto.CustomGameResponse> listCustomByServer(Long serverId) {
        Server srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다"));
        return customGameRepo.findByServer(srv).stream()
                .map(cg -> new GameDto.CustomGameResponse(cg.getId(), cg.getName()))
                .collect(Collectors.toList());
    }

    /* ---------- 커스텀 게임 추가 / 삭제 ---------- */

    public GameDto.CustomGameResponse addCustomGame(Long serverId, GameDto.CustomGameRequest req) {
        Server srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다"));

        CustomGame cg = CustomGame.builder()
                .name(req.getName())
                .server(srv)
                .build();

        cg = customGameRepo.save(cg);
        return new GameDto.CustomGameResponse(cg.getId(), cg.getName());
    }

    /**
     * 1) 커스텀 게임을 예약해 놓은 유저 목록 조회
     */
    public GameDto.ScheduledUserListResponse listScheduledUsers(Long serverId, Long gameId) {
        Server srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다"));
        CustomGame cg = customGameRepo.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게임을 찾을 수 없습니다"));

        if (!cg.getServer().getId().equals(serverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 서버의 게임이 아닙니다");
        }

        List<GameDto.ScheduledUserResponse> users = entryRepo.findByCustomGame(cg).stream()
                .map(e -> e.getUser().getUsername())
                .distinct()
                .map(GameDto.ScheduledUserResponse::new)
                .collect(Collectors.toList());

        return new GameDto.ScheduledUserListResponse(users);
    }

    /**
     * 2) 커스텀 게임 삭제 (엔트리 먼저 삭제 → 게임 삭제)
     */
    @Transactional
    public void deleteCustomGame(Long serverId, Long gameId) {
        Server srv = serverRepo.findById(serverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다"));
        CustomGame cg = customGameRepo.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게임을 찾을 수 없습니다"));

        // 권한 검사 (owner 또는 admin)
        String current = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!(srv.getOwner().getUsername().equals(current) || srv.getAdmins().stream()
                .anyMatch(u -> u.getUsername().equals(current)))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다");
        }

        // ① 해당 게임을 예약한 모든 타임테이블 엔트리 삭제
        entryRepo.deleteAllByCustomGame(cg);

        // ② 커스텀 게임 자체 삭제
        customGameRepo.delete(cg);
    }
}
