package com.example.scheduler.controller;

import com.example.scheduler.dto.PartyDto;
import com.example.scheduler.service.PartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/parties")
@RequiredArgsConstructor
@Tag(name = "Party", description = "파티 관리 API")
public class PartyController {

    private final PartyService partyService;

    @Operation(summary = "파티 생성", description = "새 파티를 생성합니다")
    @PostMapping
    public ResponseEntity<PartyDto.Response> create(
            @PathVariable Long serverId,
            @Valid @RequestBody PartyDto.CreateRequest req) {
        req.setServerId(serverId);
        return ResponseEntity.ok(partyService.create(req));
    }

    @Operation(summary = "파티 목록", description = "서버의 모든 파티를 조회합니다")
    @GetMapping
    public ResponseEntity<List<PartyDto.Response>> list(@PathVariable Long serverId) {
        return ResponseEntity.ok(partyService.list(serverId));
    }

    @Operation(summary = "파티 참가", description = "파티에 참가합니다")
    @PostMapping("/{partyId}/join")
    public ResponseEntity<PartyDto.Response> join(@PathVariable Long serverId, @PathVariable Long partyId) {
        return ResponseEntity.ok(partyService.join(partyId));
    }

    @Operation(summary = "파티 탈퇴", description = "파티에서 나갑니다")
    @PostMapping("/{partyId}/leave")
    public ResponseEntity<PartyDto.Response> leave(@PathVariable Long serverId, @PathVariable Long partyId) {
        return ResponseEntity.ok(partyService.leaveParty(partyId));
    }

    @Operation(summary = "파티 삭제", description = "파티를 삭제합니다 (파티장만 가능)")
    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> delete(@PathVariable Long serverId, @PathVariable Long partyId) {
        partyService.deletePartyEndpoint(partyId);
        return ResponseEntity.noContent().build();
    }
}


