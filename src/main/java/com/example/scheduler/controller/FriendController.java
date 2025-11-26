package com.example.scheduler.controller;

import com.example.scheduler.dto.FriendDto;
import com.example.scheduler.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Tag(name = "Friend", description = "친구 관리 API")
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 요청 보내기", description = "아이디 또는 친구코드로 친구 요청을 보냅니다")
    @PostMapping("/requests")
    public ResponseEntity<?> sendRequest(Authentication auth,
                                         @Valid @RequestBody FriendDto.SendRequest req) {
        friendService.sendRequest(auth.getName(), req.getReceiverId(), req.getFriendCode());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "친구 요청 응답", description = "받은 친구 요청을 수락하거나 거절합니다")
    @PostMapping("/requests/{id}/respond")
    public ResponseEntity<?> respond(Authentication auth,
                                     @PathVariable Long id,
                                     @Valid @RequestBody FriendDto.RespondRequest req) {
        friendService.respond(auth.getName(), id, req.isAccept());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내 친구 목록", description = "현재 사용자의 친구 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<FriendDto.FriendListResponse> listFriends(Authentication auth) {
        return ResponseEntity.ok(friendService.listFriends(auth.getName()));
    }

    @Operation(summary = "받은 친구 요청", description = "대기 중인 받은 친구 요청 목록을 조회합니다")
    @GetMapping("/requests/received")
    public ResponseEntity<FriendDto.PendingListResponse> listPendingReceived(Authentication auth) {
        return ResponseEntity.ok(friendService.listPendingReceived(auth.getName()));
    }

    @Operation(summary = "보낸 친구 요청", description = "대기 중인 보낸 친구 요청 목록을 조회합니다")
    @GetMapping("/requests/sent")
    public ResponseEntity<FriendDto.PendingListResponse> listPendingSent(Authentication auth) {
        return ResponseEntity.ok(friendService.listPendingSent(auth.getName()));
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다")
    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Void> deleteFriend(Authentication auth, @PathVariable Long friendUserId) {
        friendService.deleteFriend(auth.getName(), friendUserId);
        return ResponseEntity.noContent().build();
    }
}


