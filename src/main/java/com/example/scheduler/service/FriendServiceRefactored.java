package com.example.scheduler.service;

import com.example.scheduler.common.base.BaseService;
import com.example.scheduler.common.exception.BadRequestException;
import com.example.scheduler.common.exception.ForbiddenException;
import com.example.scheduler.common.exception.NotFoundException;
import com.example.scheduler.domain.*;
import com.example.scheduler.dto.FriendDto;
import com.example.scheduler.repository.FriendRequestRepository;
import com.example.scheduler.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 친구 관련 비즈니스 로직
 * BaseService를 상속받아 공통 기능 활용
 */
@Service("friendServiceRefactored")
@RequiredArgsConstructor
public class FriendServiceRefactored extends BaseService {

    private final FriendRequestRepository requestRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    // ==================== 친구 요청 ====================

    /**
     * 친구 요청 전송
     *
     * @param fromUsername 요청자 username
     * @param toUserId     수신자 ID (friendCode와 둘 중 하나 필수)
     * @param friendCode   수신자 친구코드 (toUserId와 둘 중 하나 필수)
     */
    @Transactional
    public void sendRequest(String fromUsername, Long toUserId, String friendCode) {
        User sender = findUserByUsername(fromUsername);
        User receiver = resolveReceiver(toUserId, friendCode);

        validateSendRequest(sender, receiver);

        // 상대가 이미 보낸 대기 요청이 있으면 바로 수락 처리
        if (hasOppositeRequest(sender, receiver)) {
            FriendRequest oppositeRequest = requestRepository.findBySenderAndReceiver(receiver, sender).get();
            acceptInternal(oppositeRequest);
            return;
        }

        FriendRequest request = createOrUpdateRequest(sender, receiver);
        sendFriendRequestNotification(sender, receiver, request);
    }

    /**
     * 친구 요청 응답 (수락/거절)
     */
    @Transactional
    public void respond(String toUsername, Long requestId, boolean accept) {
        User me = findUserByUsername(toUsername);
        FriendRequest request = findRequestById(requestId);

        validateCanRespond(request, me);

        if (accept) {
            acceptInternal(request);
            notifyRequestAccepted(request, me);
        } else {
            rejectInternal(request);
            notifyRequestRejected(request, me);
        }

        deleteRequestNotification(requestId);
    }

    // ==================== 친구 목록 조회 ====================

    /**
     * 친구 목록 조회
     */
    @Transactional(readOnly = true)
    public FriendDto.FriendListResponse listFriends(String username) {
        User me = findUserByUsername(username);

        // 양방향 관계 모두 조회
        List<Friendship> asUser = friendshipRepository.findByUser(me);
        List<Friendship> asFriend = friendshipRepository.findByFriend(me);

        // 중복 제거 (동일 친구가 양방향에 모두 존재할 수 있음)
        Map<Long, FriendDto.SimpleUser> uniqueFriends = new LinkedHashMap<>();

        asUser.forEach(f -> {
            User friend = f.getFriend();
            uniqueFriends.putIfAbsent(friend.getId(), toSimpleUser(friend));
        });

        asFriend.forEach(f -> {
            User friend = f.getUser();
            uniqueFriends.putIfAbsent(friend.getId(), toSimpleUser(friend));
        });

        return new FriendDto.FriendListResponse(new ArrayList<>(uniqueFriends.values()));
    }

    /**
     * 받은 친구 요청 목록
     */
    @Transactional(readOnly = true)
    public FriendDto.PendingListResponse listPendingReceived(String username) {
        User me = findUserByUsername(username);

        List<FriendDto.PendingRequest> requests = requestRepository
                .findByReceiverAndStatus(me, FriendRequestStatus.PENDING)
                .stream()
                .map(r -> new FriendDto.PendingRequest(r.getId(), toSimpleUser(r.getSender())))
                .collect(Collectors.toList());

        return new FriendDto.PendingListResponse(requests);
    }

    /**
     * 보낸 친구 요청 목록
     */
    @Transactional(readOnly = true)
    public FriendDto.PendingListResponse listPendingSent(String username) {
        User me = findUserByUsername(username);

        List<FriendDto.PendingRequest> requests = requestRepository
                .findBySenderAndStatus(me, FriendRequestStatus.PENDING)
                .stream()
                .map(r -> new FriendDto.PendingRequest(r.getId(), toSimpleUser(r.getReceiver())))
                .collect(Collectors.toList());

        return new FriendDto.PendingListResponse(requests);
    }

    // ==================== 친구 삭제 ====================

    /**
     * 친구 삭제 (양방향)
     */
    @Transactional
    public void deleteFriend(String username, Long friendUserId) {
        User me = findUserByUsername(username);
        User friend = findUserById(friendUserId);

        // 양방향 관계 모두 삭제
        friendshipRepository.deleteByUserAndFriend(me, friend);
        friendshipRepository.deleteByUserAndFriend(friend, me);
    }

    // ==================== Private 헬퍼 메서드 ====================

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(NotFoundException::user);
    }

    private User findUserByFriendCode(String friendCode) {
        return userRepository.findByFriendCode(friendCode)
                .orElseThrow(NotFoundException::friendRequest);
    }

    private FriendRequest findRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(NotFoundException::friendRequest);
    }

    private User resolveReceiver(Long toUserId, String friendCode) {
        if (friendCode != null && !friendCode.isBlank()) {
            return findUserByFriendCode(friendCode);
        } else if (toUserId != null) {
            return findUserById(toUserId);
        } else {
            throw BadRequestException.invalidInput("대상 정보가 필요합니다");
        }
    }

    private void validateSendRequest(User sender, User receiver) {
        // 자기 자신에게 요청 불가
        if (sender.getId().equals(receiver.getId())) {
            throw BadRequestException.friendCannotAddSelf();
        }

        // 이미 친구인지 확인
        boolean alreadyFriends = friendshipRepository.existsByUserAndFriend(sender, receiver)
                || friendshipRepository.existsByUserAndFriend(receiver, sender);
        if (alreadyFriends) {
            throw BadRequestException.friendAlreadyExists();
        }
    }

    private boolean hasOppositeRequest(User sender, User receiver) {
        return requestRepository.findBySenderAndReceiver(receiver, sender)
                .map(req -> req.getStatus() == FriendRequestStatus.PENDING)
                .orElse(false);
    }

    private FriendRequest createOrUpdateRequest(User sender, User receiver) {
        var existingOpt = requestRepository.findBySenderAndReceiver(sender, receiver);

        if (existingOpt.isPresent()) {
            FriendRequest existing = existingOpt.get();
            if (existing.getStatus() == FriendRequestStatus.PENDING) {
                throw BadRequestException.friendRequestAlreadySent();
            }
            // 거절된 요청은 PENDING으로 재설정하여 재전송 허용
            existing.setStatus(FriendRequestStatus.PENDING);
            return requestRepository.save(existing);
        }

        FriendRequest newRequest = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();
        return requestRepository.save(newRequest);
    }

    private void validateCanRespond(FriendRequest request, User user) {
        if (!request.getReceiver().getId().equals(user.getId())) {
            throw new ForbiddenException(
                    com.example.scheduler.common.exception.ErrorCode.ACCESS_DENIED,
                    "내가 받은 요청만 응답할 수 있습니다"
            );
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw BadRequestException.invalidInput("이미 처리된 요청입니다");
        }
    }

    private void acceptInternal(FriendRequest request) {
        request.setStatus(FriendRequestStatus.ACCEPTED);
        requestRepository.save(request);

        // 양방향 친구 관계 생성
        Friendship forward = Friendship.builder()
                .user(request.getSender())
                .friend(request.getReceiver())
                .build();
        Friendship backward = Friendship.builder()
                .user(request.getReceiver())
                .friend(request.getSender())
                .build();

        friendshipRepository.save(forward);
        friendshipRepository.save(backward);
    }

    private void rejectInternal(FriendRequest request) {
        request.setStatus(FriendRequestStatus.REJECTED);
        requestRepository.save(request);
    }

    // ==================== 알림 관련 ====================

    private void sendFriendRequestNotification(User sender, User receiver, FriendRequest request) {
        String payload = String.format(
                "{\"kind\":\"friend_request\",\"requestId\":%d,\"fromNickname\":\"%s\"}",
                request.getId(), sender.getNickname()
        );
        String title = String.format("%s님이 친구 요청을 보냈어요", sender.getNickname());

        notificationService.notify(receiver, NotificationType.GENERIC, title, payload);
    }

    private void notifyRequestAccepted(FriendRequest request, User accepter) {
        String title = String.format("%s님이 친구 요청을 수락했어요", accepter.getNickname());
        notificationService.notify(request.getSender(), NotificationType.GENERIC, title, null);
    }

    private void notifyRequestRejected(FriendRequest request, User rejecter) {
        String title = String.format("%s님이 친구 요청을 거절했어요", rejecter.getNickname());
        notificationService.notify(request.getSender(), NotificationType.GENERIC, title, null);
    }

    private void deleteRequestNotification(Long requestId) {
        notificationService.deleteMineByMessageFragment(
                NotificationType.GENERIC,
                "\"requestId\":" + requestId
        );
    }

    // ==================== DTO 변환 ====================

    private FriendDto.SimpleUser toSimpleUser(User user) {
        return new FriendDto.SimpleUser(
                user.getId(),
                user.getUsername(),
                user.getNickname()
        );
    }
}
