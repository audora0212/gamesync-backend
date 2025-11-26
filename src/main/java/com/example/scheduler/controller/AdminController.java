package com.example.scheduler.controller;

import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.TimetableEntry;
import com.example.scheduler.domain.Party;
import com.example.scheduler.dto.AdminDto;
import com.example.scheduler.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AuditLogRepository auditRepo;
    private final ServerRepository serverRepo;
    private final UserRepository userRepo;
    private final TimetableEntryRepository entryRepo;
    private final PartyRepository partyRepo;
    private final DefaultGameRepository defaultGameRepo;
    private final CustomGameRepository customGameRepo;

    // ----- Audit logs -----
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AdminDto.AuditLogItem>> listAuditLogs(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "serverId", required = false) Long serverId,
            @RequestParam(value = "action", required = false) String action
    ) {
        java.util.Set<String> actions = null;
        if (action != null && !action.isBlank()) {
            actions = java.util.Set.of(action);
        } else if (category != null) {
            switch (category) {
                case "server" -> actions = java.util.Set.of(
                        "CREATE_SERVER", "JOIN_SERVER", "LEAVE_SERVER", "KICK_MEMBER", "CHANGE_ADMIN"
                );
                case "timetable" -> actions = java.util.Set.of(
                        "TIMETABLE_REGISTER", "TIMETABLE_UPDATE", "TIMETABLE_DELETE", "TIMETABLE_RESET_DELETE"
                );
                case "party" -> actions = java.util.Set.of(
                        "PARTY_CREATE", "PARTY_JOIN", "PARTY_LEAVE", "PARTY_DELETE"
                );
                default -> actions = null;
            }
        }

        var stream = auditRepo.findAll().stream();
        if (serverId != null) {
            stream = stream.filter(l -> serverId.equals(l.getServerId()));
        }
        if (actions != null && !actions.isEmpty()) {
            java.util.Set<String> finalActions = actions;
            stream = stream.filter(l -> finalActions.contains(l.getAction()));
        }
        var list = stream.map(l -> new AdminDto.AuditLogItem(
                l.getId(), l.getServerId(), l.getUserId(),
                (l.getUserId()!=null? userRepo.findById(l.getUserId()).map(u->u.getNickname()).orElse(null):null),
                l.getAction(), l.getDetails(), l.getOccurredAt()
        )).toList();
        return ResponseEntity.ok(list);
    }

    // 서버별 참가 기록(Join) 조회
    @GetMapping("/servers/{id}/join-logs")
    public ResponseEntity<List<AdminDto.AuditLogItem>> listServerJoinLogs(@PathVariable("id") Long id) {
        var list = auditRepo.findAll().stream()
                .filter(l -> id.equals(l.getServerId()))
                .filter(l -> "JOIN_SERVER".equals(l.getAction()))
                .map(l -> new AdminDto.AuditLogItem(
                        l.getId(), l.getServerId(), l.getUserId(),
                        (l.getUserId()!=null? userRepo.findById(l.getUserId()).map(u->u.getNickname()).orElse(null):null),
                        l.getAction(), l.getDetails(), l.getOccurredAt()
                )).toList();
        return ResponseEntity.ok(list);
    }

    // ----- Servers -----
    @GetMapping("/servers")
    public ResponseEntity<List<AdminDto.ServerItem>> listServers() {
        var list = serverRepo.findAll().stream().map(s -> new AdminDto.ServerItem(
                s.getId(), s.getName(), s.getOwner()!=null?s.getOwner().getId():null, s.getOwner()!=null?s.getOwner().getNickname():null,
                s.getResetTime(), s.getMembers()!=null?s.getMembers().size():0
        )).toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/servers/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 서버 수정 (DTO로 안전하게 처리)
    @PutMapping("/servers")
    public ResponseEntity<AdminDto.ServerItem> upsertServer(@Valid @RequestBody AdminDto.ServerUpsertRequest req) {
        Server server;
        if (req.getId() != null) {
            server = serverRepo.findById(req.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다"));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "관리자 API로 새 서버 생성은 지원되지 않습니다");
        }
        // 허용된 필드만 업데이트
        server.setName(req.getName());
        server.setResetTime(req.getResetTime());
        server.setDescription(req.getDescription());
        server.setMaxMembers(req.getMaxMembers());
        server.setResetPaused(req.isResetPaused());
        serverRepo.save(server);

        return ResponseEntity.ok(new AdminDto.ServerItem(
                server.getId(), server.getName(),
                server.getOwner() != null ? server.getOwner().getId() : null,
                server.getOwner() != null ? server.getOwner().getNickname() : null,
                server.getResetTime(),
                server.getMembers() != null ? server.getMembers().size() : 0
        ));
    }

    // ----- Timetables -----
    @GetMapping("/timetables")
    public ResponseEntity<List<AdminDto.TimetableItem>> listTimetables() {
        var list = entryRepo.findAll().stream().map(e -> new AdminDto.TimetableItem(
                e.getId(),
                e.getServer()!=null?e.getServer().getId():null,
                e.getServer()!=null?e.getServer().getName():null,
                e.getUser()!=null?e.getUser().getId():null,
                e.getUser()!=null?e.getUser().getNickname():null,
                e.getSlot(),
                e.getCustomGame()!=null?e.getCustomGame().getName():(e.getDefaultGame()!=null?e.getDefaultGame().getName():null)
        )).toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/timetables/{id}")
    public ResponseEntity<Void> deleteTimetable(@PathVariable Long id) {
        entryRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 타임테이블 수정 (DTO로 안전하게 처리)
    @PutMapping("/timetables")
    public ResponseEntity<AdminDto.TimetableItem> upsertTimetable(@Valid @RequestBody AdminDto.TimetableUpsertRequest req) {
        TimetableEntry entry;
        if (req.getId() != null) {
            entry = entryRepo.findById(req.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "타임테이블 엔트리를 찾을 수 없습니다"));
        } else {
            entry = new TimetableEntry();
        }

        // 연관 엔티티 조회
        var server = serverRepo.findById(req.getServerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다"));
        var user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        // 허용된 필드만 업데이트
        entry.setServer(server);
        entry.setUser(user);
        entry.setSlot(req.getSlot());
        entry.setDefaultGame(req.getDefaultGameId() != null ? defaultGameRepo.findById(req.getDefaultGameId()).orElse(null) : null);
        entry.setCustomGame(req.getCustomGameId() != null ? customGameRepo.findById(req.getCustomGameId()).orElse(null) : null);
        entryRepo.save(entry);

        String gameName = entry.getCustomGame() != null ? entry.getCustomGame().getName() :
                (entry.getDefaultGame() != null ? entry.getDefaultGame().getName() : null);

        return ResponseEntity.ok(new AdminDto.TimetableItem(
                entry.getId(),
                server.getId(), server.getName(),
                user.getId(), user.getNickname(),
                entry.getSlot(), gameName
        ));
    }

    // ----- Parties -----
    @GetMapping("/parties")
    public ResponseEntity<List<AdminDto.PartyItem>> listParties() { 
        var list = partyRepo.findAll().stream().map(p -> new AdminDto.PartyItem(
                p.getId(),
                p.getServer()!=null?p.getServer().getId():null,
                p.getServer()!=null?p.getServer().getName():null,
                p.getCreator()!=null?p.getCreator().getId():null,
                p.getCreator()!=null?p.getCreator().getNickname():null,
                p.getSlot(), p.getCapacity(),
                p.getCustomGame()!=null?p.getCustomGame().getName():(p.getDefaultGame()!=null?p.getDefaultGame().getName():null),
                p.getParticipants()!=null?p.getParticipants().size():0
        )).toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/parties/{id}")
    public ResponseEntity<Void> deleteParty(@PathVariable Long id) {
        partyRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 파티 수정 (DTO로 안전하게 처리)
    @PutMapping("/parties")
    public ResponseEntity<AdminDto.PartyItem> upsertParty(@Valid @RequestBody AdminDto.PartyUpsertRequest req) {
        Party party;
        if (req.getId() != null) {
            party = partyRepo.findById(req.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파티를 찾을 수 없습니다"));
        } else {
            party = Party.builder().createdAt(LocalDateTime.now()).build();
        }

        // 연관 엔티티 조회
        var server = serverRepo.findById(req.getServerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다"));
        var creator = userRepo.findById(req.getCreatorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "생성자를 찾을 수 없습니다"));

        // 허용된 필드만 업데이트
        party.setServer(server);
        party.setCreator(creator);
        party.setSlot(req.getSlot());
        party.setCapacity(req.getCapacity());
        party.setDefaultGame(req.getDefaultGameId() != null ? defaultGameRepo.findById(req.getDefaultGameId()).orElse(null) : null);
        party.setCustomGame(req.getCustomGameId() != null ? customGameRepo.findById(req.getCustomGameId()).orElse(null) : null);
        partyRepo.save(party);

        String gameName = party.getCustomGame() != null ? party.getCustomGame().getName() :
                (party.getDefaultGame() != null ? party.getDefaultGame().getName() : null);

        return ResponseEntity.ok(new AdminDto.PartyItem(
                party.getId(),
                server.getId(), server.getName(),
                creator.getId(), creator.getNickname(),
                party.getSlot(), party.getCapacity(), gameName,
                party.getParticipants() != null ? party.getParticipants().size() : 0
        ));
    }
}


