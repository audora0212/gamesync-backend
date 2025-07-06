package com.example.scheduler.scheduler;

import com.example.scheduler.domain.Server;
import com.example.scheduler.repository.ServerRepository;
import com.example.scheduler.repository.TimetableEntryRepository;
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

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void resetTimetables() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        List<Server> servers = serverRepo.findByResetTime(now);
        for (Server srv : servers) {
            entryRepo.deleteAllByServer(srv);
        }
    }
}