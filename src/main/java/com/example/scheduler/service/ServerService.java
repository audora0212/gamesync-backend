package com.example.scheduler.service;

import com.example.scheduler.domain.CustomGame;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.ServerDto;
import com.example.scheduler.repository.CustomGameRepository;
import com.example.scheduler.repository.FriendshipRepository;
import com.example.scheduler.repository.ServerInviteRepository;
import com.example.scheduler.repository.ServerRepository;
import com.example.scheduler.repository.TimetableEntryRepository;
import com.example.scheduler.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepo;
    private final UserRepository userRepo;
    private final TimetableEntryRepository entryRepo;
    private final CustomGameRepository customGameRepo;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final boolean AuditEnable=true; //감사로그 온오프
    private final ServerInviteRepository inviteRepo;
    private final FriendshipRepository friendshipRepository;
    private final com.example.scheduler.repository.FavoriteServerRepository favoriteRepo;

    /* ---------- 생성 / 참가 ---------- */

    public List<ServerDto.Response> listMine() {
        User me = currentUser();
        return serverRepo.findByMembersContains(me).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ServerDto.Response> search(String q, int page, int size) {
        Page<Server> pg = serverRepo.findByNameContainingIgnoreCase(
                q == null ? "" : q,
                PageRequest.of(page, size)
        );
        return pg.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ServerDto.Response create(ServerDto.CreateRequest req) {
        User owner = currentUser();
        String code = RandomStringUtils.randomAlphanumeric(6).toUpperCase();

        Server srv = Server.builder()
                .name(req.getName())
                .owner(owner)
                .members(new java.util.HashSet<>(java.util.Set.of(owner)))
                .admins(new java.util.HashSet<>(java.util.Set.of(owner)))
                .resetTime(req.getResetTime())
                .inviteCode(code)
                .build();

        if(AuditEnable){
            auditService.log(srv.getId(), owner.getId(), "CREATE_SERVER", "name=" + req.getName());
        }
        serverRepo.save(srv);
        return toDto(srv);
    }

    public ServerDto.Response join(Long serverId) {
        User user = currentUser();
        Server srv = fetch(serverId);

        if (srv.getMembers().contains(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 참가한 서버입니다");
        }
        srv.getMembers().add(user);
        serverRepo.save(srv);
        return toDto(srv);
    }

    public ServerDto.Response joinByCode(String code) {
        Server srv = serverRepo.findByInviteCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code"));
        User user = currentUser();
        if (srv.getMembers().contains(user))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 참가한 서버입니다");
        srv.getMembers().add(user);
        serverRepo.save(srv);
        if(AuditEnable){
            auditService.log(srv.getId(), user.getId(), "JOIN_SERVER", null);
        }
        return toDto(srv);
    }

    /** 초대코드로 서버 기본정보 조회 (가입 전 확인용) */
    public ServerDto.Response lookupByCode(String code) {
        Server srv = serverRepo.findByInviteCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code"));
        return toDto(srv);
    }

    /* ---------- 일반 수정 ---------- */

    public ServerDto.Response updateResetTime(Long id, ServerDto.UpdateResetTimeRequest req) {
        Server srv = fetch(id);
        assertAdmin(srv, currentUser());

        srv.setResetTime(req.getResetTime());
        serverRepo.save(srv);
        return toDto(srv);
    }

    public ServerDto.Response rename(Long id, ServerDto.UpdateNameRequest req) {
        Server srv = fetch(id);
        assertAdmin(srv, currentUser());

        srv.setName(req.getName());
        serverRepo.save(srv);
        return toDto(srv);
    }

    public ServerDto.Response updateDescription(Long id, ServerDto.UpdateDescriptionRequest req) {
        Server srv = fetch(id);
        assertAdmin(srv, currentUser());
        srv.setDescription(req.getDescription());
        serverRepo.save(srv);
        return toDto(srv);
    }

    public ServerDto.Response updateMaxMembers(Long id, ServerDto.UpdateMaxMembersRequest req) {
        Server srv = fetch(id);
        assertAdmin(srv, currentUser());
        srv.setMaxMembers(req.getMaxMembers());
        serverRepo.save(srv);
        return toDto(srv);
    }

    public ServerDto.Response toggleResetPaused(Long id, ServerDto.ToggleResetPausedRequest req) {
        Server srv = fetch(id);
        assertAdmin(srv, currentUser());
        srv.setResetPaused(req.isPaused());
        serverRepo.save(srv);
        return toDto(srv);
    }

    /* ---------- 관리자 기능 ---------- */

    @Transactional
    public ServerDto.Response kick(Long id, ServerDto.KickRequest req) {
        Server srv = fetch(id);
        User me = currentUser();
        assertAdmin(srv, me);

        User target = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (srv.getOwner().equals(target))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버장은 강퇴할 수 없습니다");

        // 강퇴되는 사용자의 서버 내 타임테이블 기록 삭제
        entryRepo.deleteAllByServerAndUser(srv, target);

        srv.getMembers().remove(target);
        srv.getAdmins().remove(target);
        serverRepo.save(srv);
        if(AuditEnable){
            auditService.log(srv.getId(), me.getId(), "KICK_MEMBER", "targetUserId=" + req.getUserId());
        }

        return toDto(srv);
    }

    public ServerDto.Response updateAdmin(Long id, ServerDto.AdminRequest req) {
        Server srv = fetch(id);
        User me = currentUser();
        assertAdmin(srv, me);

        User target = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!srv.getMembers().contains(target))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버 멤버가 아닙니다");

        if (req.isGrant()) {
            srv.getAdmins().add(target);
        } else {
            if (srv.getOwner().equals(target))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버장은 항상 관리자입니다");
            srv.getAdmins().remove(target);
        }
        if(AuditEnable){
            String detail = (req.isGrant() ? "GRANT_ADMIN:" : "REVOKE_ADMIN:") + req.getUserId();
            auditService.log(srv.getId(), me.getId(), "CHANGE_ADMIN", detail);
        }

        serverRepo.save(srv);
        return toDto(srv);
    }

    /* ---------- 삭제 & 떠나기 ---------- */

    @Transactional
    public void delete(Long id) {
        Server srv = fetch(id);
        User me = currentUser();
        if (!srv.getOwner().equals(me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "서버장만 삭제할 수 있습니다");
        }
        // 1) 타임테이블 엔트리 삭제
        entryRepo.deleteAllByServer(srv);
        // 2) 커스텀 게임 및 관련 엔트리 삭제
        List<CustomGame> customs = customGameRepo.findByServer(srv);
        for (CustomGame cg : customs) {
            entryRepo.deleteAllByCustomGame(cg);
            customGameRepo.delete(cg);
        }
        // 3) 서버 삭제
        serverRepo.delete(srv);
    }

    @Transactional
    public void leave(Long id) {
        Server srv = fetch(id);
        User me = currentUser();
        if (srv.getOwner().equals(me)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버장은 떠날 수 없습니다");
        }
        entryRepo.deleteAllByServerAndUser(srv, me);
        srv.getMembers().remove(me);
        srv.getAdmins().remove(me);
        serverRepo.save(srv);
        if(AuditEnable){
            auditService.log(srv.getId(), me.getId(), "LEAVE_SERVER", null);
        }
    }

    /* ---------- 조회 ---------- */

    public List<ServerDto.Response> list() {
        return serverRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ServerDto.Response getDetail(Long id) {
        Server srv = fetch(id);
        User me = currentUser();
        if (!srv.getMembers().contains(me))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "서버 멤버가 아닙니다");

        return toDto(srv);
    }

    /* ---------- 초대 기능 ---------- */
    public ServerDto.InviteResponse createInvite(Long serverId, Long receiverUserId) {
        Server srv = fetch(serverId);
        User sender = currentUser();
        if (!srv.getMembers().contains(sender))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "서버 멤버만 초대 가능");

        User receiver = userRepo.findById(receiverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 이미 서버 멤버는 초대 불가
        if (srv.getMembers().contains(receiver)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 서버 멤버입니다");
        }

        boolean areFriends = friendshipRepository.existsByUserAndFriend(sender, receiver)
                || friendshipRepository.existsByUserAndFriend(receiver, sender);
        if (!areFriends)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "친구만 초대 가능");

        var existing = inviteRepo.findByServerAndSenderAndReceiver(srv, sender, receiver);
        com.example.scheduler.domain.ServerInvite inv;
        if (existing.isPresent()) {
            var e = existing.get();
            if (e.getStatus() == com.example.scheduler.domain.InviteStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 대기중 초대가 있습니다");
            }
            // ACCEPTED/REJECTED 등 처리된 초대는 재전송을 위해 PENDING으로 갱신
            e.setStatus(com.example.scheduler.domain.InviteStatus.PENDING);
            e.setCreatedAt(java.time.LocalDateTime.now());
            inv = inviteRepo.save(e);
        } else {
            inv = com.example.scheduler.domain.ServerInvite.builder()
                    .server(srv)
                    .sender(sender)
                    .receiver(receiver)
                    .status(com.example.scheduler.domain.InviteStatus.PENDING)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            inv = inviteRepo.save(inv);
        }

        // 알림: 초대 수신자에게 통지 (초대 ID를 payload 로 포함)
        String invitePayload = String.format(
                "{\"kind\":\"server_invite\",\"inviteId\":%d,\"serverName\":\"%s\",\"fromNickname\":\"%s\"}",
                inv.getId(), srv.getName(), sender.getNickname()
        );
        String title = String.format("%s 서버로 초대가 왔어요", srv.getName());
        notificationService.notify(
                receiver,
                com.example.scheduler.domain.NotificationType.INVITE,
                title + "\n" + String.format("%s님이 보냈습니다. 알림에서 확인하세요", sender.getNickname()),
                invitePayload
        );

        return toInviteDto(inv);
    }

    public java.util.List<ServerDto.InviteResponse> listMyInvites() {
        User me = currentUser();
        return inviteRepo.findByReceiverAndStatus(me, com.example.scheduler.domain.InviteStatus.PENDING)
                .stream().map(this::toInviteDto).collect(java.util.stream.Collectors.toList());
    }

    public ServerDto.InviteResponse respondInvite(Long inviteId, boolean accept) {
        User me = currentUser();
        com.example.scheduler.domain.ServerInvite inv = inviteRepo.findById(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!inv.getReceiver().getId().equals(me.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 초대만 응답 가능");
        if (inv.getStatus() != com.example.scheduler.domain.InviteStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 처리된 초대");

        if (accept) {
            inv.setStatus(com.example.scheduler.domain.InviteStatus.ACCEPTED);
            // 서버 가입 처리
            Server srv = inv.getServer();
            if (!srv.getMembers().contains(me)) {
                srv.getMembers().add(me);
                serverRepo.save(srv);
            }
            // 초대 발신자에게 수락 알림
            String title = String.format("%s님이 %s 초대를 수락했어요", me.getNickname(), inv.getServer().getName());
            notificationService.notify(
                    inv.getSender(),
                    com.example.scheduler.domain.NotificationType.INVITE,
                    title,
                    null
            );
        } else {
            inv.setStatus(com.example.scheduler.domain.InviteStatus.REJECTED);
            // 초대 발신자에게 거절 알림
            String title = String.format("%s님이 %s 초대를 거절했어요", me.getNickname(), inv.getServer().getName());
            notificationService.notify(
                    inv.getSender(),
                    com.example.scheduler.domain.NotificationType.INVITE,
                    title,
                    null
            );
        }
        inviteRepo.save(inv);

        // 내 알림 목록에서 해당 초대 알림 제거 (payload 에 inviteId 포함)
        notificationService.deleteMineByMessageFragment(
                com.example.scheduler.domain.NotificationType.INVITE,
                "\"inviteId\":" + inviteId
        );
        return toInviteDto(inv);
    }

    /* ---------- 내부 헬퍼 ---------- */

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsername(username).orElseThrow();
    }

    private Server fetch(Long id) {
        return serverRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "서버를 찾을 수 없습니다"));
    }

    private void assertAdmin(Server srv, User user) {
        if (!(srv.getOwner().equals(user) || srv.getAdmins().contains(user))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다");
        }
    }

    private ServerDto.Response toDto(Server s) {
        List<ServerDto.MemberInfo> mems = s.getMembers().stream()
                .map(u -> new ServerDto.MemberInfo(u.getId(), u.getNickname()))
                .collect(Collectors.toList());
        List<ServerDto.MemberInfo> adms = s.getAdmins().stream()
                .map(u -> new ServerDto.MemberInfo(u.getId(), u.getNickname()))
                .collect(Collectors.toList());
        return new ServerDto.Response(
                s.getId(),
                s.getName(),
                s.getOwner().getId(),
                s.getOwner().getNickname(),
                mems,
                adms,
                s.getResetTime(),
                s.getInviteCode(),
                s.getDescription(),
                s.getMaxMembers(),
                s.isResetPaused()
        );
    }

    private ServerDto.InviteResponse toInviteDto(com.example.scheduler.domain.ServerInvite inv) {
        return new ServerDto.InviteResponse(
                inv.getId(),
                inv.getServer().getId(),
                inv.getServer().getName(),
                inv.getSender().getId(),
                inv.getSender().getNickname(),
                inv.getReceiver().getId(),
                inv.getReceiver().getNickname(),
                inv.getStatus().name(),
                inv.getCreatedAt()
        );
    }

    /* ---------- 즐겨찾기 ---------- */
    @Transactional
    public void favorite(Long serverId) {
        User me = currentUser();
        Server srv = fetch(serverId);
        if (!srv.getMembers().contains(me))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "서버 멤버만 즐겨찾기 가능");
        favoriteRepo.findByUserAndServer(me, srv)
                .orElseGet(() -> favoriteRepo.save(
                        com.example.scheduler.domain.FavoriteServer.builder()
                                .user(me)
                                .server(srv)
                                .build()
                ));
    }

    @Transactional
    public void unfavorite(Long serverId) {
        User me = currentUser();
        Server srv = fetch(serverId);
        favoriteRepo.findByUserAndServer(me, srv)
                .ifPresent(fav -> favoriteRepo.delete(fav));
    }

    public List<ServerDto.Response> listMyFavorites() {
        User me = currentUser();
        return favoriteRepo.findByUser(me).stream()
                .map(fs -> toDto(fs.getServer()))
                .collect(Collectors.toList());
    }
}
