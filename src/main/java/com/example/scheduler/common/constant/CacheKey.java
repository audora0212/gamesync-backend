package com.example.scheduler.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 캐시 키 상수 정의
 * 향후 캐싱 적용 시 사용
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheKey {

    // ========== User ==========
    public static final String USER_BY_ID = "user:id:";
    public static final String USER_BY_USERNAME = "user:username:";

    // ========== Server ==========
    public static final String SERVER_BY_ID = "server:id:";
    public static final String SERVER_MEMBERS = "server:members:";
    public static final String USER_SERVERS = "user:servers:";

    // ========== Friend ==========
    public static final String USER_FRIENDS = "user:friends:";
    public static final String FRIEND_CODE = "friend:code:";

    // ========== Game ==========
    public static final String DEFAULT_GAMES = "games:default";
    public static final String SERVER_GAMES = "games:server:";

    // ========== Notification ==========
    public static final String USER_NOTIFICATIONS = "notifications:user:";
    public static final String UNREAD_COUNT = "notifications:unread:";
}
