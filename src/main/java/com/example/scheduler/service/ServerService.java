package com.example.scheduler.service;

import com.example.scheduler.domain.CustomGame;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import com.example.scheduler.dto.ServerDto;
import com.example.scheduler.repository.CustomGameRepository;
import com.example.scheduler.repository.ServerRepository;
import com.example.scheduler.repository.TimetableEntryRepository;
import com.example.scheduler.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepo;
    private final UserRepository userRepo;
    private final TimetableEntryRepository entryRepo;
    private final CustomGameRepository customGameRepo;
    private final AuditService auditService;
    private final boolean AuditEnable=true; //감사로그 온오프

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
                .members(Set.of(owner))
                .admins(Set.of(owner))
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

    /* ---------- 관리자 기능 ---------- */

    public ServerDto.Response kick(Long id, ServerDto.KickRequest req) {
        Server srv = fetch(id);
        User me = currentUser();
        assertAdmin(srv, me);

        User target = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (srv.getOwner().equals(target))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서버장은 강퇴할 수 없습니다");

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
                s.getInviteCode()
        );
    }
}
