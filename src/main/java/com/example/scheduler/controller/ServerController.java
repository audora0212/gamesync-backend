package com.example.scheduler.controller;

import com.example.scheduler.dto.ServerDto;
import com.example.scheduler.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @GetMapping("/mine")
    public ResponseEntity<List<ServerDto.Response>> listMine() {
        return ResponseEntity.ok(serverService.listMine());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ServerDto.Response>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(serverService.search(q, page, size));
    }

    @PostMapping
    public ResponseEntity<ServerDto.Response> create(@RequestBody ServerDto.CreateRequest req) {
        return ResponseEntity.ok(serverService.create(req));
    }

    @PostMapping("/{id:\\d+}/join")
    public ResponseEntity<ServerDto.Response> join(@PathVariable("id") Long id) {
        return ResponseEntity.ok(serverService.join(id));
    }

    @PostMapping("/join")
    public ResponseEntity<ServerDto.Response> joinByCode(@RequestParam String code) {
        return ResponseEntity.ok(serverService.joinByCode(code));
    }

    @GetMapping("/lookup")
    public ResponseEntity<ServerDto.Response> lookupByCode(@RequestParam String code) {
        return ResponseEntity.ok(serverService.lookupByCode(code));
    }

    @PutMapping("/{id:\\d+}/reset-time")
    public ResponseEntity<ServerDto.Response> updateResetTime(
            @PathVariable("id") Long id,
            @RequestBody ServerDto.UpdateResetTimeRequest req) {
        return ResponseEntity.ok(serverService.updateResetTime(id, req));
    }

    @PutMapping("/{id:\\d+}/name")
    public ResponseEntity<ServerDto.Response> rename(
            @PathVariable("id") Long id,
            @RequestBody ServerDto.UpdateNameRequest req) {
        return ResponseEntity.ok(serverService.rename(id, req));
    }

    @PutMapping("/{id:\\d+}/description")
    public ResponseEntity<ServerDto.Response> updateDescription(
            @PathVariable("id") Long id,
            @RequestBody ServerDto.UpdateDescriptionRequest req) {
        return ResponseEntity.ok(serverService.updateDescription(id, req));
    }

    @PutMapping("/{id:\\d+}/max-members")
    public ResponseEntity<ServerDto.Response> updateMaxMembers(
            @PathVariable("id") Long id,
            @RequestBody ServerDto.UpdateMaxMembersRequest req) {
        return ResponseEntity.ok(serverService.updateMaxMembers(id, req));
    }

    @PutMapping("/{id:\\d+}/reset-paused")
    public ResponseEntity<ServerDto.Response> toggleResetPaused(
            @PathVariable("id") Long id,
            @RequestBody ServerDto.ToggleResetPausedRequest req) {
        return ResponseEntity.ok(serverService.toggleResetPaused(id, req));
    }

    @PostMapping("/{id:\\d+}/kick")
    public ResponseEntity<ServerDto.Response> kick(
            @PathVariable("id") Long id,
            @RequestBody ServerDto.KickRequest req) {
        return ResponseEntity.ok(serverService.kick(id, req));
    }

    @PostMapping("/{id:\\d+}/admins")
    public ResponseEntity<ServerDto.Response> updateAdmin(
            @PathVariable("id") Long id,
            @RequestBody ServerDto.AdminRequest req) {
        return ResponseEntity.ok(serverService.updateAdmin(id, req));
    }

    // 서버 삭제
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        serverService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 서버 떠나기
    @PostMapping("/{id:\\d+}/leave")
    public ResponseEntity<Void> leave(@PathVariable("id") Long id) {
        serverService.leave(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ServerDto.Response>> list() {
        return ResponseEntity.ok(serverService.list());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ServerDto.Response> detail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(serverService.getDetail(id));
    }

    /* ---------- 즐겨찾기 ---------- */
    @PostMapping("/{id}/favorite")
    public ResponseEntity<Void> favorite(@PathVariable Long id) {
        serverService.favorite(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<Void> unfavorite(@PathVariable Long id) {
        serverService.unfavorite(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites/mine")
    public ResponseEntity<List<ServerDto.Response>> myFavorites() {
        return ResponseEntity.ok(serverService.listMyFavorites());
    }

    /* ---------- 초대 API ---------- */
    @PostMapping("/invites")
    public ResponseEntity<ServerDto.InviteResponse> createInvite(@RequestBody ServerDto.InviteCreateRequest req) {
        return ResponseEntity.ok(serverService.createInvite(req.getServerId(), req.getReceiverUserId()));
    }

    @GetMapping("/invites/me")
    public ResponseEntity<java.util.List<ServerDto.InviteResponse>> myInvites() {
        return ResponseEntity.ok(serverService.listMyInvites());
    }

    @PostMapping("/invites/{inviteId}/respond")
    public ResponseEntity<ServerDto.InviteResponse> respondInvite(
            @PathVariable Long inviteId,
            @RequestBody ServerDto.InviteDecisionRequest req
    ) {
        return ResponseEntity.ok(serverService.respondInvite(inviteId, req.isAccept()));
    }
}
