package com.example.scheduler.controller;

import com.example.scheduler.dto.FriendDto;
import com.example.scheduler.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /* ---------- 친구 요청 보내기 ---------- */
    @PostMapping("/requests")
    public ResponseEntity<?> sendRequest(Authentication auth,
                                         @Valid @RequestBody FriendDto.SendRequest req) {
        // 아이디 혹은 친구코드로 요청을 생성한다
        friendService.sendRequest(auth.getName(), req.getReceiverId(), req.getFriendCode());
        return ResponseEntity.ok().build();
    }

    /* ---------- 받은 요청 응답 (수락/거절) ---------- */
    @PostMapping("/requests/{id}/respond")
    public ResponseEntity<?> respond(Authentication auth,
                                     @PathVariable Long id,
                                     @Valid @RequestBody FriendDto.RespondRequest req) {
        // 내가 받은 요청인지 확인하고 수락/거절 처리한다
        friendService.respond(auth.getName(), id, req.isAccept());
        return ResponseEntity.ok().build();
    }

    /* ---------- 내 친구 목록 ---------- */
    @GetMapping
    public ResponseEntity<FriendDto.FriendListResponse> listFriends(Authentication auth) {
        return ResponseEntity.ok(friendService.listFriends(auth.getName()));
    }

    /* ---------- 대기중인 요청 (내가 받은) ---------- */
    @GetMapping("/requests/received")
    public ResponseEntity<FriendDto.PendingListResponse> listPendingReceived(Authentication auth) {
        return ResponseEntity.ok(friendService.listPendingReceived(auth.getName()));
    }

    /* ---------- 대기중인 요청 (내가 보낸) ---------- */
    @GetMapping("/requests/sent")
    public ResponseEntity<FriendDto.PendingListResponse> listPendingSent(Authentication auth) {
        return ResponseEntity.ok(friendService.listPendingSent(auth.getName()));
    }

    /* ---------- 친구 삭제 ---------- */
    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Void> deleteFriend(Authentication auth, @PathVariable Long friendUserId) {
        friendService.deleteFriend(auth.getName(), friendUserId);
        return ResponseEntity.noContent().build();
    }
}


