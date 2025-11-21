package com.example.scheduler.scheduler;

import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.TimetableEntry;
import com.example.scheduler.service.AuditService;
import com.example.scheduler.repository.ServerRepository;
import com.example.scheduler.repository.TimetableEntryRepository;
import com.example.scheduler.repository.PartyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TimetableResetScheduler {
    private final ServerRepository serverRepo;
    private final TimetableEntryRepository entryRepo;
    private final PartyRepository partyRepo;
    private final AuditService auditService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void resetTimetables() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        List<Server> servers = serverRepo.findByResetTime(now);
        for (Server srv : servers) {
            if (srv.isResetPaused()) continue;
            // 감사 로그를 위해 삭제 전 엔트리 조회 후 기록
            try {
                java.util.List<TimetableEntry> entries = entryRepo.findByServerOrderBySlot(srv);
                for (TimetableEntry e : entries) {
                    Long userId = (e.getUser() != null) ? e.getUser().getId() : null;
                    String gameName = (e.getCustomGame() != null)
                            ? e.getCustomGame().getName()
                            : (e.getDefaultGame() != null ? e.getDefaultGame().getName() : "");
                    String details = String.format("reason=SERVER_RESET;game=%s;slot=%s", safe(gameName), e.getSlot());
                    auditService.log(srv.getId(), userId, "TIMETABLE_RESET_DELETE", details);
                }
            } catch (Exception ignored) {}
            entryRepo.deleteAllByServer(srv);

            // 파티도 함께 초기화
            try {
                auditService.log(srv.getId(), null, "PARTY_RESET_DELETE", "reason=SERVER_RESET");
            } catch (Exception ignored) {}
            partyRepo.deleteAllByServer(srv);
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}