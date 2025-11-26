package com.example.scheduler.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 날짜/시간 관련 유틸리티
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateTimeUtil {

    public static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 현재 한국 시간
     */
    public static LocalDateTime nowKorea() {
        return LocalDateTime.now(KOREA_ZONE);
    }

    /**
     * 현재 한국 날짜
     */
    public static LocalDate todayKorea() {
        return LocalDate.now(KOREA_ZONE);
    }

    /**
     * 현재 한국 시간(시각만)
     */
    public static LocalTime timeNowKorea() {
        return LocalTime.now(KOREA_ZONE);
    }

    /**
     * 오늘 특정 시간의 LocalDateTime 생성
     */
    public static LocalDateTime todayAt(int hour, int minute) {
        return LocalDateTime.of(todayKorea(), LocalTime.of(hour, minute));
    }

    /**
     * 날짜 문자열 파싱
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMAT);
    }

    /**
     * 시간 문자열 파싱
     */
    public static LocalTime parseTime(String timeStr) {
        return LocalTime.parse(timeStr, TIME_FORMAT);
    }

    /**
     * LocalDateTime을 문자열로 변환
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMAT) : null;
    }

    /**
     * LocalDate를 문자열로 변환
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : null;
    }

    /**
     * LocalTime을 문자열로 변환
     */
    public static String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMAT) : null;
    }

    /**
     * 두 시간 사이의 분 차이 계산
     */
    public static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        return Duration.between(from, to).toMinutes();
    }

    /**
     * 시간이 특정 범위 내에 있는지 확인
     */
    public static boolean isTimeBetween(LocalTime target, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !target.isBefore(start) && target.isBefore(end);
        } else {
            // 자정을 넘어가는 경우 (예: 23:00 ~ 02:00)
            return !target.isBefore(start) || target.isBefore(end);
        }
    }
}
