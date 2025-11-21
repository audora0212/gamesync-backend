package com.example.scheduler.controller;

import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.TimetableEntry;
import com.example.scheduler.domain.Party;
import com.example.scheduler.dto.AdminDto;
import com.example.scheduler.repository.AuditLogRepository;
import com.example.scheduler.repository.ServerRepository;
import com.example.scheduler.repository.TimetableEntryRepository;
import com.example.scheduler.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AuditLogRepository auditRepo;
    private final ServerRepository serverRepo;
    private final com.example.scheduler.repository.UserRepository userRepo;
    private final TimetableEntryRepository entryRepo;
    private final PartyRepository partyRepo;

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

    // 최소 예: 이름/리셋시간 수정은 기존 ServerService를 쓰는 게 안전하지만, 간단히 저장
    @PutMapping("/servers")
    public ResponseEntity<Server> upsertServer(@RequestBody Server s) {
        return ResponseEntity.ok(serverRepo.save(s));
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

    @PutMapping("/timetables")
    public ResponseEntity<TimetableEntry> upsertTimetable(@RequestBody TimetableEntry e) {
        return ResponseEntity.ok(entryRepo.save(e));
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

    @PutMapping("/parties")
    public ResponseEntity<Party> upsertParty(@RequestBody Party p) { return ResponseEntity.ok(partyRepo.save(p)); }
}


