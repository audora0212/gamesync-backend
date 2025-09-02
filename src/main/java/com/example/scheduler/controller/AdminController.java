package com.example.scheduler.controller;

import com.example.scheduler.domain.AuditLog;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.TimetableEntry;
import com.example.scheduler.domain.Party;
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
    private final TimetableEntryRepository entryRepo;
    private final PartyRepository partyRepo;

    // ----- Audit logs -----
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> listAuditLogs() {
        return ResponseEntity.ok(auditRepo.findAll());
    }

    // ----- Servers -----
    @GetMapping("/servers")
    public ResponseEntity<List<Server>> listServers() { return ResponseEntity.ok(serverRepo.findAll()); }

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
    public ResponseEntity<List<TimetableEntry>> listTimetables() { return ResponseEntity.ok(entryRepo.findAll()); }

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
    public ResponseEntity<List<Party>> listParties() { return ResponseEntity.ok(partyRepo.findAll()); }

    @DeleteMapping("/parties/{id}")
    public ResponseEntity<Void> deleteParty(@PathVariable Long id) {
        partyRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/parties")
    public ResponseEntity<Party> upsertParty(@RequestBody Party p) { return ResponseEntity.ok(partyRepo.save(p)); }
}


