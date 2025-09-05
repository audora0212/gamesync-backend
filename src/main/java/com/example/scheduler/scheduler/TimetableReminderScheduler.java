package com.example.scheduler.scheduler;

import com.example.scheduler.domain.NotificationType;
import com.example.scheduler.domain.TimetableEntry;
import com.example.scheduler.domain.User;
import com.example.scheduler.repository.TimetableEntryRepository;
import com.example.scheduler.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TimetableReminderScheduler {

    private final TimetableEntryRepository entryRepo;
    private final NotificationService notificationService;

    public TimetableReminderScheduler(TimetableEntryRepository entryRepo,
                                      NotificationService notificationService) {
        this.entryRepo = entryRepo;
        this.notificationService = notificationService;
    }

    // 1분마다 체크
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        // 오늘과 내일 사이 전체 엔트리 조회 후, 각 유저의 설정을 기준으로 now + offset 일치 여부 확인
        // 간단화를 위해 전체 엔트리 가져와 필터링
        // 실제 서비스에서는 시간 인덱싱/범위 조회로 최적화 권장
        List<TimetableEntry> all = entryRepo.findAll();
        for (TimetableEntry e : all) {
            User u = e.getUser();
            Boolean enabled = u.getPushMyTimetableReminderEnabled();
            if (Boolean.FALSE.equals(enabled)) continue;
            Integer minutes = u.getMyTimetableReminderMinutes();
            if (minutes == null) minutes = 10;
            LocalDateTime target = e.getSlot().minusMinutes(minutes).withSecond(0).withNano(0);
            if (target.equals(now)) {
                String title = "곧 합류 시간입니다";
                String body = e.getCustomGame() != null ? e.getCustomGame().getName() : e.getDefaultGame().getName();
                body = body + " · " + e.getSlot().toLocalTime().toString();
                notificationService.notifyPushOnly(u, NotificationType.TIMETABLE, title, body);
            }
        }
    }
}


