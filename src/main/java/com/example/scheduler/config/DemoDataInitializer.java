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
import java.util.concurrent.ThreadLocalRandom;

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

        // 1) 유저 5명 생성(존재하면 재사용) - 자연스러운 닉네임 사용
        String[] nicknames = {"민수", "수연", "재현", "유진", "현우"};
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int idx = i;
            String uname = "demo" + idx;
            String nname = nicknames[idx - 1];
            User u = userRepo.findByUsername(uname).orElseGet(() ->
                    userRepo.save(User.builder()
                            .username(uname)
                            .nickname(nname)
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

        // 5) 이번 주 자연스러운 패턴으로 엔트리/로그 생성
        LocalDate monday = LocalDate.now().minusDays((LocalDate.now().getDayOfWeek().getValue() + 6) % 7);
        for (int d = 0; d < 7; d++) { // 주 7일
            LocalDate day = monday.plusDays(d);
            int dow = day.getDayOfWeek().getValue(); // 1..7

            // 각 유저: 주중 ~60%, 주말 ~80% 확률로 참여, 즐겨하는 게임 60% + 랜덤 40%
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                double p = (dow >= 6) ? 0.8 : 0.6; // 토/일 가중
                if (ThreadLocalRandom.current().nextDouble() > p) continue; // 쉬는 날

                int favIdx = i % games.size();
                boolean useFav = ThreadLocalRandom.current().nextDouble() < 0.6;
                DefaultGame g = games.get(useFav ? favIdx : ThreadLocalRandom.current().nextInt(games.size()));

                int base = (dow >= 5) ? 20 : 19; // 금/토는 조금 늦게 시작 경향
                int addUpperExclusive = Math.max(1, 24 - base); // hour < 24 보장
                int hour = base + ThreadLocalRandom.current().nextInt(0, addUpperExclusive);
                int[] mins = {0, 15, 30, 45};
                int minute = mins[ThreadLocalRandom.current().nextInt(mins.length)];
                LocalDateTime slot = LocalDateTime.of(day, LocalTime.of(hour, minute));

                entryRepo.findByServerAndUser(srv, u).ifPresent(entryRepo::delete);
                TimetableEntry e = TimetableEntry.builder()
                        .server(srv)
                        .user(u)
                        .defaultGame(g)
                        .slot(slot)
                        .build();
                entryRepo.save(e);

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

            // 테스트 유저: 주중 50%, 주말 70% 확률로 참여, 40%는 고정 즐겨찾기, 나머지 랜덤
            double pt = (dow >= 6) ? 0.7 : 0.5;
            if (ThreadLocalRandom.current().nextDouble() < pt) {
                boolean fav = ThreadLocalRandom.current().nextDouble() < 0.4;
                DefaultGame g0 = games.get(fav ? 0 : ThreadLocalRandom.current().nextInt(games.size()));
                int base = (dow >= 5) ? 21 : 20;
                int addUpperExclusive = Math.max(1, 24 - base);
                int hour = base + ThreadLocalRandom.current().nextInt(0, Math.min(3, addUpperExclusive));
                int[] mins = {0, 10, 20, 30, 40, 50};
                int minute = mins[ThreadLocalRandom.current().nextInt(mins.length)];
                LocalDateTime tslot = LocalDateTime.of(day, LocalTime.of(hour, minute));

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
}


