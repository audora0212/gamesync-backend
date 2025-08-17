package com.example.scheduler.controller;

import com.example.scheduler.dto.PartyDto;
import com.example.scheduler.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/parties")
@RequiredArgsConstructor
public class PartyController {
    private final PartyService partyService;

    @PostMapping
    public ResponseEntity<PartyDto.Response> create(@PathVariable Long serverId, @RequestBody PartyDto.CreateRequest req) {
        req.setServerId(serverId);
        return ResponseEntity.ok(partyService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<PartyDto.Response>> list(@PathVariable Long serverId) {
        return ResponseEntity.ok(partyService.list(serverId));
    }

    @PostMapping("/{partyId}/join")
    public ResponseEntity<PartyDto.Response> join(@PathVariable Long serverId, @PathVariable Long partyId) {
        return ResponseEntity.ok(partyService.join(partyId));
    }
}


