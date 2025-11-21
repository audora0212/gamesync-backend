// repository/TimetableEntryRepository.java
package com.example.scheduler.repository;

import com.example.scheduler.domain.CustomGame;
import com.example.scheduler.domain.TimetableEntry;
import com.example.scheduler.domain.Server;
import com.example.scheduler.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findByServerOrderBySlot(Server server);
    List<TimetableEntry> findByServerAndSlot(Server server, LocalDateTime slot);
    Optional<TimetableEntry> findByServerAndUser(Server server, User user);
    void deleteAllByServer(Server server);

    // ↓ 추가 ↓
    /** 특정 CustomGame을 예약한 모든 엔트리 */
    List<TimetableEntry> findByCustomGame(CustomGame customGame);

    /** 특정 CustomGame을 예약한 엔트리 전부 삭제 */
    void deleteAllByCustomGame(CustomGame customGame);

    void deleteAllByServerAndUser(Server server, User user);
}
