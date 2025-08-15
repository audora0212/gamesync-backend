package com.example.scheduler.service;

import com.example.scheduler.domain.*;
import com.example.scheduler.dto.FriendDto;
import com.example.scheduler.repository.FriendRequestRepository;
import com.example.scheduler.repository.FriendshipRepository;
import com.example.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final UserRepository userRepository;
    private final FriendRequestRepository requestRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    private User getUserOr404(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User getUserByUsernameOr404(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void sendRequest(String fromUsername, Long toUserId, String friendCode) {
        User sender = getUserByUsernameOr404(fromUsername);
        User receiver;

        if (friendCode != null && !friendCode.isBlank()) {
            receiver = userRepository.findByFriendCode(friendCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코드에 해당하는 유저 없음"));
        } else if (toUserId != null) {
            receiver = getUserOr404(toUserId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대상 정보가 비어있다");
        }

        if (sender.getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신에게는 못 보낸다");
        }

        // 이미 친구인지 확인 (양방향 저장 구조이므로 두 방향 모두 체크)
        boolean alreadyFriends = friendshipRepository.existsByUserAndFriend(sender, receiver)
                || friendshipRepository.existsByUserAndFriend(receiver, sender);
        if (alreadyFriends) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 친구다");
        }

        // 상대가 보낸 대기중 요청이 있으면 바로 수락 처리하고 종료
        var oppositeOpt = requestRepository.findBySenderAndReceiver(receiver, sender);
        if (oppositeOpt.isPresent() && oppositeOpt.get().getStatus() == FriendRequestStatus.PENDING) {
            acceptInternal(oppositeOpt.get());
            return;
        }

        // 기존 요청 확인: PENDING이면 차단, REJECTED면 재전송을 위해 PENDING으로 갱신
        var existingOpt = requestRepository.findBySenderAndReceiver(sender, receiver);
        FriendRequest req;
        if (existingOpt.isPresent()) {
            var existing = existingOpt.get();
            if (existing.getStatus() == FriendRequestStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 보낸 요청이 있다");
            }
            // REJECTED 등 처리된 요청은 상태를 PENDING으로 되돌려 재전송 허용
            existing.setStatus(FriendRequestStatus.PENDING);
            req = requestRepository.save(existing);
        } else {
            req = FriendRequest.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .status(FriendRequestStatus.PENDING)
                    .build();
            requestRepository.save(req);
        }

        // 수신자에게 알림 (친구 요청)
        String payload = String.format(
                "{\"kind\":\"friend_request\",\"requestId\":%d,\"fromNickname\":\"%s\"}",
                req.getId(), sender.getNickname()
        );
        String title = String.format("%s님이 친구 요청을 보냈어요", sender.getNickname());

        // 저장형 알림 + 푸시 발송
        notificationService.notify(
                receiver,
                com.example.scheduler.domain.NotificationType.GENERIC,
                title,
                payload
        );
    }

    @Transactional
    public void respond(String toUsername, Long requestId, boolean accept) {
        User me = getUserByUsernameOr404(toUsername);
        FriendRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!req.getReceiver().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 받은 요청만 응답 가능");
        }
        if (req.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 처리된 요청");
        }

        if (accept) {
            acceptInternal(req);
            // 발신자에게 수락 알림
            String title = String.format("%s님이 친구 요청을 수락했어요", me.getNickname());
            notificationService.notify(
                    req.getSender(),
                    com.example.scheduler.domain.NotificationType.GENERIC,
                    title,
                    null
            );
        } else {
            req.setStatus(FriendRequestStatus.REJECTED);
            requestRepository.save(req);
            // 발신자에게 거절 알림
            String title = String.format("%s님이 친구 요청을 거절했어요", me.getNickname());
            notificationService.notify(
                    req.getSender(),
                    com.example.scheduler.domain.NotificationType.GENERIC,
                    title,
                    null
            );
        }

        // 내 알림 목록에서 해당 친구요청 알림 제거 (payload 에 requestId 포함)
        notificationService.deleteMineByMessageFragment(
                com.example.scheduler.domain.NotificationType.GENERIC,
                "\"requestId\":" + requestId
        );
    }

    private void acceptInternal(FriendRequest req) {
        req.setStatus(FriendRequestStatus.ACCEPTED);
        requestRepository.save(req);

        // 친구 관계를 양방향으로 저장
        Friendship a = Friendship.builder().user(req.getSender()).friend(req.getReceiver()).build();
        Friendship b = Friendship.builder().user(req.getReceiver()).friend(req.getSender()).build();
        friendshipRepository.save(a);
        friendshipRepository.save(b);
    }

    @Transactional(readOnly = true)
    public FriendDto.FriendListResponse listFriends(String username) {
        User me = getUserByUsernameOr404(username);
        List<Friendship> a = friendshipRepository.findByUser(me);
        List<Friendship> b = friendshipRepository.findByFriend(me); // 혹시 한쪽만 저장되어 있는 경우 커버

        // 중복 제거: 같은 상대가 a/b 양쪽에서 나올 수 있으므로 ID 기준으로 중복 제거
        Map<Long, FriendDto.SimpleUser> uniqueById = new LinkedHashMap<>();

        a.forEach(f -> {
            var friend = f.getFriend();
            uniqueById.put(friend.getId(), new FriendDto.SimpleUser(
                    friend.getId(),
                    friend.getUsername(),
                    friend.getNickname()
            ));
        });

        b.forEach(f -> {
            var friend = f.getUser();
            uniqueById.put(friend.getId(), new FriendDto.SimpleUser(
                    friend.getId(),
                    friend.getUsername(),
                    friend.getNickname()
            ));
        });

        return new FriendDto.FriendListResponse(new ArrayList<>(uniqueById.values()));
    }

    @Transactional(readOnly = true)
    public FriendDto.PendingListResponse listPendingReceived(String username) {
        User me = getUserByUsernameOr404(username);
        List<FriendRequest> list = requestRepository.findByReceiverAndStatus(me, FriendRequestStatus.PENDING);
        List<FriendDto.PendingRequest> requests = list.stream()
                .map(r -> new FriendDto.PendingRequest(
                        r.getId(),
                        new FriendDto.SimpleUser(
                                r.getSender().getId(),
                                r.getSender().getUsername(),
                                r.getSender().getNickname()
                        )
                ))
                .collect(Collectors.toList());
        return new FriendDto.PendingListResponse(requests);
    }

    @Transactional(readOnly = true)
    public FriendDto.PendingListResponse listPendingSent(String username) {
        User me = getUserByUsernameOr404(username);
        List<FriendRequest> list = requestRepository.findBySenderAndStatus(me, FriendRequestStatus.PENDING);
        List<FriendDto.PendingRequest> requests = list.stream()
                .map(r -> new FriendDto.PendingRequest(
                        r.getId(),
                        new FriendDto.SimpleUser(
                                r.getReceiver().getId(),
                                r.getReceiver().getUsername(),
                                r.getReceiver().getNickname()
                        )
                ))
                .collect(Collectors.toList());
        return new FriendDto.PendingListResponse(requests);
    }

    /** 양방향 친구 관계 삭제 */
    @Transactional
    public void deleteFriend(String username, Long friendUserId) {
        User me = getUserByUsernameOr404(username);
        User friend = getUserOr404(friendUserId);
        // 양방향 모두 삭제 (한쪽만 저장된 경우도 대비)
        friendshipRepository.deleteByUserAndFriend(me, friend);
        friendshipRepository.deleteByUserAndFriend(friend, me);
    }
}


