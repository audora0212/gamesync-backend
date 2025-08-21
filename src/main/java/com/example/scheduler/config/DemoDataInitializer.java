package com.example.scheduler.config;

import com.example.scheduler.domain.*;
import com.example.scheduler.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final UserRepository userRepo;
    private final ServerRepository serverRepo;
    private final DefaultGameRepository defaultGameRepo;
    private final TimetableEntryRepository entryRepo;
    private final FriendshipRepository friendshipRepo;
    
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // 0) 테스트 로그인 계정 보장: id=test / pw=asdfasdf
        User testUser = userRepo.findByUsername("test").orElseGet(() -> {
            User t = User.builder()
                    .username("test")
                    .nickname("테스트")
                    .password(passwordEncoder.encode("asdfasdf"))
                    .email("test@example.com")
                    .notificationsEnabled(true)
                    .build();
            return userRepo.save(t);
        });

        // 이후 로직: 데모 서버는 없으면 생성, 있으면 멤버 구성만 보정

        // 1) 유저 5명 생성(존재하면 재사용)
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int idx = i;
            String uname = "demo" + idx;
            User u = userRepo.findByUsername(uname).orElseGet(() ->
                    userRepo.save(User.builder()
                            .username(uname)
                            .nickname("데모" + idx)
                            .password(passwordEncoder.encode("demo" + idx + "-pass"))
                            .email(uname + "@example.com")
                            .notificationsEnabled(true)
                            .build())
            );
            users.add(u);
        }

        // 2) 서버 생성 및 멤버 추가
        User owner = users.get(0);
        Server srv = serverRepo.findByInviteCode("DEMO01").orElse(null);
        if (srv == null) {
            srv = Server.builder()
                    .name("Demo Server")
                    .owner(owner)
                    .members(new HashSet<>(users))
                    .admins(new HashSet<>(List.of(owner)))
                    .inviteCode("DEMO01")
                    .resetTime(LocalTime.of(5, 0))
                    .build();
        } else {
            if (srv.getMembers() == null) srv.setMembers(new HashSet<>());
            srv.getMembers().addAll(users);
        }
        // 테스트 계정도 멤버로 포함
        srv.getMembers().add(testUser);
        srv = serverRepo.save(srv);

        // 2-1) 수집중 배지 방지용 선행 로그(데모 전용): 이번주 시작 이전에 하나 생성 (집계엔 영향 없음)
        var preWeekLog = AuditLog.builder()
                .serverId(srv.getId())
                .userId(owner.getId())
                .action("TIMETABLE_REGISTER")
                .details("")
                .occurredAt(LocalDate.now().minusDays((LocalDate.now().getDayOfWeek().getValue() + 6) % 7)
                        .atStartOfDay().minusMinutes(1))
                .build();
        auditRepo.save(preWeekLog);

        // 3) 친구 관계(완전 연결이 아니라, 시연용으로 체인 연결)
        for (int i = 0; i < users.size() - 1; i++) {
            User a = users.get(i);
            User b = users.get(i + 1);
            if (!friendshipRepo.existsByUserAndFriend(a, b))
                friendshipRepo.save(Friendship.builder().user(a).friend(b).build());
            if (!friendshipRepo.existsByUserAndFriend(b, a))
                friendshipRepo.save(Friendship.builder().user(b).friend(a).build());
        }

        // 4) 기본 게임 확보(없으면 생성)
        String[] gameNames = {"VALORANT", "League of Legends", "Overwatch 2", "MapleStory", "PUBG"};
        List<DefaultGame> games = new ArrayList<>();
        for (String g : gameNames) {
            DefaultGame dg = defaultGameRepo.findByName(g).orElseGet(() -> defaultGameRepo.save(DefaultGame.builder().name(g).build()));
            games.add(dg);
        }

        // 5) 이번 주 월~일 중 저녁 시간에 엔트리 생성 + 감사 로그 기록
        LocalDate monday = LocalDate.now().minusDays((LocalDate.now().getDayOfWeek().getValue() + 6) % 7);
        for (int d = 0; d < 7; d++) { // 주 7일
            LocalDate day = monday.plusDays(d);
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                DefaultGame g = games.get(i % games.size());
                LocalTime time = LocalTime.of(20 + (i % 3), 0); // 20:00~22:00 사이
                LocalDateTime slot = LocalDateTime.of(day, time);

                // 기존 중복 제거
                entryRepo.findByServerAndUser(srv, u).ifPresent(entryRepo::delete);

                TimetableEntry e = TimetableEntry.builder()
                        .server(srv)
                        .user(u)
                        .defaultGame(g)
                        .slot(slot)
                        .build();
                entryRepo.save(e);

                // 감사 로그(집계용) - occurredAt = slot 시간으로 저장
                auditRepo.save(
                        AuditLog.builder()
                                .serverId(srv.getId())
                                .userId(u.getId())
                                .action("TIMETABLE_REGISTER")
                                .details("game=" + g.getName() + ";slot=" + slot)
                                .occurredAt(slot)
                                .build()
                );
            }
            // 테스트 유저 일정도 일부 추가
            DefaultGame g0 = games.get(0);
            LocalDateTime tslot = LocalDateTime.of(day, LocalTime.of(21, 0));
            entryRepo.findByServerAndUser(srv, testUser).ifPresent(entryRepo::delete);
            TimetableEntry te = TimetableEntry.builder()
                    .server(srv)
                    .user(testUser)
                    .defaultGame(g0)
                    .slot(tslot)
                    .build();
            entryRepo.save(te);
            auditRepo.save(
                    AuditLog.builder()
                            .serverId(srv.getId())
                            .userId(testUser.getId())
                            .action("TIMETABLE_REGISTER")
                            .details("game=" + g0.getName() + ";slot=" + tslot)
                            .occurredAt(tslot)
                            .build()
            );
        }
    }
}


