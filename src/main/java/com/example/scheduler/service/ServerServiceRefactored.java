package com.example.scheduler.service;

import com.example.scheduler.common.base.BaseService;
import com.example.scheduler.common.constant.AuditAction;
import com.example.scheduler.common.exception.BadRequestException;
import com.example.scheduler.common.exception.ForbiddenException;
import com.example.scheduler.common.exception.NotFoundException;
import com.example.scheduler.domain.*;
import com.example.scheduler.dto.ServerDto;
import com.example.scheduler.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 서버 관련 비즈니스 로직
 * BaseService를 상속받아 공통 기능 활용
 */
@Service("serverServiceRefactored")
@RequiredArgsConstructor
public class ServerServiceRefactored extends BaseService {

    private final ServerRepository serverRepo;
    private final TimetableEntryRepository entryRepo;
    private final CustomGameRepository customGameRepo;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ServerInviteRepository inviteRepo;
    private final FriendshipRepository friendshipRepo;
    private final FavoriteServerRepository favoriteRepo;

    private static final boolean AUDIT_ENABLED = true;

    // ==================== 서버 생성/참가 ====================

    /**
     * 내가 속한 서버 목록 조회
     */
    public List<ServerDto.Response> listMine() {
        User me = getCurrentUser();
        return serverRepo.findByMembersContains(me).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 서버 검색
     */
    public List<ServerDto.Response> search(String query, int page, int size) {
        String searchQuery = query == null ? "" : query;
        Page<Server> result = serverRepo.findByNameContainingIgnoreCase(
                searchQuery, PageRequest.of(page, size));
        return result.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 서버 생성
     */
    public ServerDto.Response create(ServerDto.CreateRequest req) {
        User owner = getCurrentUser();
        String inviteCode = generateInviteCode();

        Server server = Server.builder()
                .name(req.getName())
                .owner(owner)
                .members(new HashSet<>(Set.of(owner)))
                .admins(new HashSet<>(Set.of(owner)))
                .resetTime(req.getResetTime())
                .inviteCode(inviteCode)
                .build();

        serverRepo.save(server);
        audit(server.getId(), owner.getId(), AuditAction.CREATE_SERVER, "name=" + req.getName());

        return toDto(server);
    }

    /**
     * 서버 참가 (ID로)
     */
    public ServerDto.Response join(Long serverId) {
        User user = getCurrentUser();
        Server server = findServerById(serverId);

        validateNotAlreadyMember(server, user);
        server.getMembers().add(user);
        serverRepo.save(server);

        return toDto(server);
    }

    /**
     * 서버 참가 (초대코드로)
     */
    public ServerDto.Response joinByCode(String code) {
        Server server = findServerByInviteCode(code);
        User user = getCurrentUser();

        validateNotAlreadyMember(server, user);
        server.getMembers().add(user);
        serverRepo.save(server);
        audit(server.getId(), user.getId(), AuditAction.JOIN_SERVER, null);

        return toDto(server);
    }

    /**
     * 초대코드로 서버 정보 조회 (가입 전 확인용)
     */
    public ServerDto.Response lookupByCode(String code) {
        Server server = findServerByInviteCode(code);
        return toDto(server);
    }

    // ==================== 서버 설정 수정 ====================

    /**
     * 리셋 시간 수정
     */
    public ServerDto.Response updateResetTime(Long id, ServerDto.UpdateResetTimeRequest req) {
        Server server = findServerById(id);
        assertAdmin(server);

        server.setResetTime(req.getResetTime());
        serverRepo.save(server);
        return toDto(server);
    }

    /**
     * 서버 이름 수정
     */
    public ServerDto.Response rename(Long id, ServerDto.UpdateNameRequest req) {
        Server server = findServerById(id);
        assertAdmin(server);

        server.setName(req.getName());
        serverRepo.save(server);
        return toDto(server);
    }

    /**
     * 서버 설명 수정
     */
    public ServerDto.Response updateDescription(Long id, ServerDto.UpdateDescriptionRequest req) {
        Server server = findServerById(id);
        assertAdmin(server);

        server.setDescription(req.getDescription());
        serverRepo.save(server);
        return toDto(server);
    }

    /**
     * 최대 멤버 수 수정
     */
    public ServerDto.Response updateMaxMembers(Long id, ServerDto.UpdateMaxMembersRequest req) {
        Server server = findServerById(id);
        assertAdmin(server);

        server.setMaxMembers(req.getMaxMembers());
        serverRepo.save(server);
        return toDto(server);
    }

    /**
     * 리셋 일시정지 토글
     */
    public ServerDto.Response toggleResetPaused(Long id, ServerDto.ToggleResetPausedRequest req) {
        Server server = findServerById(id);
        assertAdmin(server);

        server.setResetPaused(req.isPaused());
        serverRepo.save(server);
        return toDto(server);
    }

    // ==================== 관리자 기능 ====================

    /**
     * 멤버 강퇴
     */
    @Transactional
    public ServerDto.Response kick(Long id, ServerDto.KickRequest req) {
        Server server = findServerById(id);
        User me = getCurrentUser();
        assertAdmin(server);

        User target = findUserById(req.getUserId());
        validateCanKick(server, target);

        // 강퇴 대상의 타임테이블 기록 삭제
        entryRepo.deleteAllByServerAndUser(server, target);

        server.getMembers().remove(target);
        server.getAdmins().remove(target);
        serverRepo.save(server);

        audit(server.getId(), me.getId(), AuditAction.KICK_MEMBER, "targetUserId=" + req.getUserId());
        return toDto(server);
    }

    /**
     * 관리자 권한 부여/해제
     */
    public ServerDto.Response updateAdmin(Long id, ServerDto.AdminRequest req) {
        Server server = findServerById(id);
        User me = getCurrentUser();
        assertAdmin(server);

        User target = findUserById(req.getUserId());
        validateIsMember(server, target);

        if (req.isGrant()) {
            server.getAdmins().add(target);
        } else {
            validateCanRevokeAdmin(server, target);
            server.getAdmins().remove(target);
        }

        String action = req.isGrant() ? AuditAction.GRANT_ADMIN : AuditAction.REVOKE_ADMIN;
        audit(server.getId(), me.getId(), AuditAction.CHANGE_ADMIN, action + ":" + req.getUserId());

        serverRepo.save(server);
        return toDto(server);
    }

    // ==================== 삭제 & 떠나기 ====================

    /**
     * 서버 삭제 (서버장만 가능)
     */
    @Transactional
    public void delete(Long id) {
        Server server = findServerById(id);
        User me = getCurrentUser();
        assertOwner(server);

        // 연관 데이터 삭제
        entryRepo.deleteAllByServer(server);

        List<CustomGame> customGames = customGameRepo.findByServer(server);
        for (CustomGame game : customGames) {
            entryRepo.deleteAllByCustomGame(game);
            customGameRepo.delete(game);
        }

        serverRepo.delete(server);
    }

    /**
     * 서버 탈퇴
     */
    @Transactional
    public void leave(Long id) {
        Server server = findServerById(id);
        User me = getCurrentUser();

        if (server.getOwner().equals(me)) {
            throw BadRequestException.serverOwnerCannotLeave();
        }

        entryRepo.deleteAllByServerAndUser(server, me);
        server.getMembers().remove(me);
        server.getAdmins().remove(me);
        serverRepo.save(server);

        audit(server.getId(), me.getId(), AuditAction.LEAVE_SERVER, null);
    }

    // ==================== 조회 ====================

    /**
     * 전체 서버 목록 조회
     */
    public List<ServerDto.Response> list() {
        return serverRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 서버 상세 조회
     */
    public ServerDto.Response getDetail(Long id) {
        Server server = findServerById(id);
        User me = getCurrentUser();

        if (!server.getMembers().contains(me)) {
            throw ForbiddenException.serverNotMember();
        }
        return toDto(server);
    }

    // ==================== 초대 기능 ====================

    /**
     * 서버 초대 생성
     */
    public ServerDto.InviteResponse createInvite(Long serverId, Long receiverUserId) {
        Server server = findServerById(serverId);
        User sender = getCurrentUser();

        validateCanInvite(server, sender);

        User receiver = findUserById(receiverUserId);
        validateInviteReceiver(server, sender, receiver);

        ServerInvite invite = getOrCreateInvite(server, sender, receiver);

        // 알림 발송
        sendInviteNotification(server, sender, receiver, invite);

        return toInviteDto(invite);
    }

    /**
     * 받은 초대 목록 조회
     */
    public List<ServerDto.InviteResponse> listMyInvites() {
        User me = getCurrentUser();
        return inviteRepo.findByReceiverAndStatus(me, InviteStatus.PENDING)
                .stream()
                .map(this::toInviteDto)
                .collect(Collectors.toList());
    }

    /**
     * 초대 응답 (수락/거절)
     */
    public ServerDto.InviteResponse respondInvite(Long inviteId, boolean accept) {
        User me = getCurrentUser();
        ServerInvite invite = findInviteById(inviteId);

        validateCanRespondInvite(invite, me);

        if (accept) {
            acceptInvite(invite, me);
        } else {
            rejectInvite(invite, me);
        }

        inviteRepo.save(invite);
        deleteInviteNotification(inviteId);

        return toInviteDto(invite);
    }

    // ==================== 즐겨찾기 ====================

    @Transactional
    public void favorite(Long serverId) {
        User me = getCurrentUser();
        Server server = findServerById(serverId);

        if (!server.getMembers().contains(me)) {
            throw ForbiddenException.serverNotMember();
        }

        favoriteRepo.findByUserAndServer(me, server)
                .orElseGet(() -> favoriteRepo.save(
                        FavoriteServer.builder()
                                .user(me)
                                .server(server)
                                .build()
                ));
    }

    @Transactional
    public void unfavorite(Long serverId) {
        User me = getCurrentUser();
        Server server = findServerById(serverId);

        favoriteRepo.findByUserAndServer(me, server)
                .ifPresent(favoriteRepo::delete);
    }

    public List<ServerDto.Response> listMyFavorites() {
        User me = getCurrentUser();
        return favoriteRepo.findByUser(me).stream()
                .map(fs -> toDto(fs.getServer()))
                .collect(Collectors.toList());
    }

    // ==================== Private 헬퍼 메서드 ====================

    private Server findServerById(Long id) {
        return serverRepo.findById(id)
                .orElseThrow(NotFoundException::server);
    }

    private Server findServerByInviteCode(String code) {
        return serverRepo.findByInviteCode(code)
                .orElseThrow(BadRequestException::serverInvalidInviteCode);
    }

    private ServerInvite findInviteById(Long id) {
        return inviteRepo.findById(id)
                .orElseThrow(NotFoundException::invite);
    }

    private String generateInviteCode() {
        return RandomStringUtils.randomAlphanumeric(6).toUpperCase();
    }

    private void assertAdmin(Server server) {
        User me = getCurrentUser();
        if (!(server.getOwner().equals(me) || server.getAdmins().contains(me))) {
            throw ForbiddenException.serverAdminRequired();
        }
    }

    private void assertOwner(Server server) {
        User me = getCurrentUser();
        if (!server.getOwner().equals(me)) {
            throw ForbiddenException.serverOwnerRequired();
        }
    }

    private void validateNotAlreadyMember(Server server, User user) {
        if (server.getMembers().contains(user)) {
            throw BadRequestException.serverAlreadyMember();
        }
    }

    private void validateIsMember(Server server, User user) {
        if (!server.getMembers().contains(user)) {
            throw ForbiddenException.serverNotMember();
        }
    }

    private void validateCanKick(Server server, User target) {
        if (server.getOwner().equals(target)) {
            throw BadRequestException.serverOwnerCannotKick();
        }
    }

    private void validateCanRevokeAdmin(Server server, User target) {
        if (server.getOwner().equals(target)) {
            throw BadRequestException.serverOwnerAlwaysAdmin();
        }
    }

    private void validateCanInvite(Server server, User sender) {
        if (!server.getMembers().contains(sender)) {
            throw ForbiddenException.inviteOnlyMember();
        }
    }

    private void validateInviteReceiver(Server server, User sender, User receiver) {
        if (server.getMembers().contains(receiver)) {
            throw BadRequestException.serverAlreadyMember();
        }

        boolean areFriends = friendshipRepo.existsByUserAndFriend(sender, receiver)
                || friendshipRepo.existsByUserAndFriend(receiver, sender);
        if (!areFriends) {
            throw ForbiddenException.inviteOnlyFriends();
        }
    }

    private ServerInvite getOrCreateInvite(Server server, User sender, User receiver) {
        var existing = inviteRepo.findByServerAndSenderAndReceiver(server, sender, receiver);

        if (existing.isPresent()) {
            ServerInvite invite = existing.get();
            if (invite.getStatus() == InviteStatus.PENDING) {
                throw BadRequestException.inviteAlreadyPending();
            }
            // 이미 처리된 초대는 재전송을 위해 PENDING으로 갱신
            invite.setStatus(InviteStatus.PENDING);
            invite.setCreatedAt(LocalDateTime.now());
            return inviteRepo.save(invite);
        }

        ServerInvite newInvite = ServerInvite.builder()
                .server(server)
                .sender(sender)
                .receiver(receiver)
                .status(InviteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return inviteRepo.save(newInvite);
    }

    private void validateCanRespondInvite(ServerInvite invite, User user) {
        if (!invite.getReceiver().getId().equals(user.getId())) {
            throw ForbiddenException.inviteNotReceiver();
        }
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw BadRequestException.inviteAlreadyProcessed();
        }
    }

    private void acceptInvite(ServerInvite invite, User me) {
        invite.setStatus(InviteStatus.ACCEPTED);

        Server server = invite.getServer();
        if (!server.getMembers().contains(me)) {
            server.getMembers().add(me);
            serverRepo.save(server);
        }

        // 초대자에게 알림
        String title = String.format("%s님이 %s 초대를 수락했어요", me.getNickname(), server.getName());
        notificationService.notify(invite.getSender(), NotificationType.INVITE, title, null);
    }

    private void rejectInvite(ServerInvite invite, User me) {
        invite.setStatus(InviteStatus.REJECTED);

        // 초대자에게 알림
        String title = String.format("%s님이 %s 초대를 거절했어요", me.getNickname(), invite.getServer().getName());
        notificationService.notify(invite.getSender(), NotificationType.INVITE, title, null);
    }

    private void sendInviteNotification(Server server, User sender, User receiver, ServerInvite invite) {
        String payload = String.format(
                "{\"kind\":\"server_invite\",\"inviteId\":%d,\"serverName\":\"%s\",\"fromNickname\":\"%s\"}",
                invite.getId(), server.getName(), sender.getNickname()
        );
        String title = String.format("%s 서버로 초대가 왔어요", server.getName());
        String body = String.format("%s님이 보냈습니다. 알림에서 확인하세요", sender.getNickname());

        notificationService.notify(receiver, NotificationType.INVITE, title + "\n" + body, payload);
    }

    private void deleteInviteNotification(Long inviteId) {
        notificationService.deleteMineByMessageFragment(
                NotificationType.INVITE,
                "\"inviteId\":" + inviteId
        );
    }

    private void audit(Long serverId, Long userId, String action, String details) {
        if (AUDIT_ENABLED) {
            auditService.log(serverId, userId, action, details);
        }
    }

    // ==================== DTO 변환 ====================

    private ServerDto.Response toDto(Server server) {
        List<ServerDto.MemberInfo> members = server.getMembers().stream()
                .map(u -> new ServerDto.MemberInfo(u.getId(), u.getNickname()))
                .collect(Collectors.toList());

        List<ServerDto.MemberInfo> admins = server.getAdmins().stream()
                .map(u -> new ServerDto.MemberInfo(u.getId(), u.getNickname()))
                .collect(Collectors.toList());

        return new ServerDto.Response(
                server.getId(),
                server.getName(),
                server.getOwner().getId(),
                server.getOwner().getNickname(),
                members,
                admins,
                server.getResetTime(),
                server.getInviteCode(),
                server.getDescription(),
                server.getMaxMembers(),
                server.isResetPaused()
        );
    }

    private ServerDto.InviteResponse toInviteDto(ServerInvite invite) {
        return new ServerDto.InviteResponse(
                invite.getId(),
                invite.getServer().getId(),
                invite.getServer().getName(),
                invite.getSender().getId(),
                invite.getSender().getNickname(),
                invite.getReceiver().getId(),
                invite.getReceiver().getNickname(),
                invite.getStatus().name(),
                invite.getCreatedAt()
        );
    }
}
