package com.example.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class StatsDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameCount {
        private String name;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourCount {
        private int hour; // 0-23
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCount {
        private Long userId;
        private String nickname;
        private long count; // days or actions
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayAvg {
        private int dow; // 1=Mon .. 7=Sun (ISO)
        private int avgMinuteOfDay; // 0..1439
        private long sampleCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayGames {
        private int dow; // 1..7
        private java.util.List<NameCount> items;
    }

    /* ---- Today ---- */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayStatsResponse {
        private String topGame; // null if none
        private int avgMinuteOfDay; // 0..1439
        private int peakHour; // 0..23
        private long peakHourCount;
        private java.util.List<HourCount> hourlyCounts;
    }

    /* ---- Weekly ---- */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyStatsResponse {
        private java.util.List<UserCount> topUsers; // top 3 by active days (max 7)
        private java.util.List<DayAvg> dowAvg; // 1..7
        private java.util.List<DayGames> dowGames; // 1..7
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregatedResponse {
        private String range; // weekly only
        private LocalDateTime start;
        private LocalDateTime end;
        private String topGame;
        private int topHour;
        private long topHourCount;
        private List<NameCount> topGames; // desc
        private List<HourCount> hourCounts; // size 24
        private boolean collecting; // 주간 데이터 수집중 여부
    }
}


