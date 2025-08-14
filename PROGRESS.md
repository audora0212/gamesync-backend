## Gamesync Backend Progress

팀이 날짜별로 어떤 기능을 구현/변경했는지 기록합니다. 각 항목은 실제 소스의 파일/모듈 기준으로 작성합니다.

### 업데이트 방법
- 새로운 작업이 배포되거나 메인 브랜치에 머지되면 아래 표에 한 줄을 추가합니다.
- 형식: 날짜(YYYY-MM-DD), 담당, 작업 범위, 요약, 관련 파일/모듈, 상태
- 가능하면 커밋/PR 번호, 이슈 번호를 요약에 함께 적습니다. (예: PR #12, commit abc1234)

### 변경 이력

| 날짜 | 담당 | 작업 범위 | 요약 | 관련 파일/모듈 | 상태 |
| --- | --- | --- | --- | --- | --- |
| 2025-08-13 | gy255 | 문서 | 진행 문서 초안 추가 및 표준화 | `PROGRESS.md` | 완료 |
| 2025-08-12 | team | 스케줄러 | 만료 토큰 정리, 시간표 주간 초기화 스케줄러 구현 | `src/main/java/com/example/scheduler/scheduler/BlacklistCleanupScheduler.java`, `src/main/java/com/example/scheduler/scheduler/TimetableResetScheduler.java` | 완료 |
| 2025-08-11 | team | 시간표 | 유저 시간표 CRUD 및 주간 리셋 연동 | `src/main/java/com/example/scheduler/domain/TimetableEntry.java`, `src/main/java/com/example/scheduler/repository/TimetableEntryRepository.java`, `src/main/java/com/example/scheduler/service/TimetableService.java`, `src/main/java/com/example/scheduler/controller/TimetableController.java` | 완료 |
| 2025-08-10 | team | 알림/푸시 | FCM 연동, 푸시 토큰 등록/삭제 및 알림 발송 API | `src/main/java/com/example/scheduler/config/FirebaseConfig.java`, `src/main/java/com/example/scheduler/service/PushService.java`, `src/main/java/com/example/scheduler/service/NotificationService.java`, `src/main/java/com/example/scheduler/controller/NotificationController.java`, `src/main/java/com/example/scheduler/controller/PushTokenController.java`, `src/main/java/com/example/scheduler/README_FCM.md` | 완료 |
| 2025-08-09 | team | 서버(길드) | 서버 생성/조회, 초대/가입, 통계 기초 엔드포인트 | `src/main/java/com/example/scheduler/domain/Server.java`, `src/main/java/com/example/scheduler/domain/ServerInvite.java`, `src/main/java/com/example/scheduler/repository/ServerRepository.java`, `src/main/java/com/example/scheduler/repository/ServerInviteRepository.java`, `src/main/java/com/example/scheduler/service/ServerService.java`, `src/main/java/com/example/scheduler/controller/ServerController.java` | 완료 |
| 2025-08-08 | team | 친구 | 친구 요청/수락/차단 흐름 및 알림 설정 | `src/main/java/com/example/scheduler/domain/FriendRequest.java`, `src/main/java/com/example/scheduler/domain/Friendship.java`, `src/main/java/com/example/scheduler/domain/FriendNotificationSetting.java`, `src/main/java/com/example/scheduler/repository/FriendRequestRepository.java`, `src/main/java/com/example/scheduler/repository/FriendshipRepository.java`, `src/main/java/com/example/scheduler/repository/FriendNotificationSettingRepository.java`, `src/main/java/com/example/scheduler/service/FriendService.java`, `src/main/java/com/example/scheduler/service/FriendCodeService.java`, `src/main/java/com/example/scheduler/controller/FriendController.java` | 완료 |
| 2025-08-07 | team | 게임 관리 | 기본 게임 초기화, 커스텀 게임 CRUD, 사용자 선호 관리 | `src/main/java/com/example/scheduler/domain/DefaultGame.java`, `src/main/java/com/example/scheduler/domain/CustomGame.java`, `src/main/java/com/example/scheduler/service/GameService.java`, `src/main/java/com/example/scheduler/controller/GameController.java`, `src/main/resources/default_games.txt`, `src/main/java/com/example/scheduler/config/DefaultGameInitializer.java` | 완료 |
| 2025-08-06 | team | 감사 로깅 | 주요 보안/관리 이벤트 감사 로깅 | `src/main/java/com/example/scheduler/domain/AuditLog.java`, `src/main/java/com/example/scheduler/repository/AuditLogRepository.java`, `src/main/java/com/example/scheduler/service/AuditService.java` | 완료 |
| 2025-08-05 | team | 인증/보안 | JWT 발급/검증, Discord OAuth2, 보안 설정/필터 | `src/main/java/com/example/scheduler/security/JwtTokenProvider.java`, `src/main/java/com/example/scheduler/security/JwtAuthenticationFilter.java`, `src/main/java/com/example/scheduler/security/OAuth2LoginSuccessHandler.java`, `src/main/java/com/example/scheduler/security/SecurityConfig.java`, `src/main/java/com/example/scheduler/service/AuthService.java`, `src/main/java/com/example/scheduler/service/CustomUserDetailsService.java`, `src/main/java/com/example/scheduler/service/DiscordOAuth2UserService.java`, `src/main/java/com/example/scheduler/controller/AuthController.java` | 완료 |
| 2025-08-04 | team | 사용자 | 사용자 프로필/닉네임 조회·수정, 중복 검사 | `src/main/java/com/example/scheduler/domain/User.java`, `src/main/java/com/example/scheduler/repository/UserRepository.java`, `src/main/java/com/example/scheduler/service/UserService.java`, `src/main/java/com/example/scheduler/controller/UserController.java` | 완료 |
| 2025-08-03 | team | 프로젝트 | Gradle/Spring Boot 초기화, 패키지 구조 정리 | `build.gradle`, `settings.gradle`, `src/main/java/com/example/scheduler/SchedulerApplication.java`, `src/main/java/com/example/scheduler/controller/*`, `src/main/java/com/example/scheduler/service/*`, `src/main/java/com/example/scheduler/repository/*`, `src/main/java/com/example/scheduler/domain/*` | 완료 |

### 작업 체크리스트
- [x] 서버 이름 변경 기능
- [x] 서버 초기화 시간 수정
- [x] 멤버 강퇴
- [x] 관리자 권한 부여,해제
- [x] ‘/server’ 페이지 컴포넌트 글래스 처리
- [x] ‘/stats’ 페이지 컴포넌트 글래스 처리
- [x] ‘/dashboard’ 페이지 ‘입장’버튼 크기 상하폭 늘리기
- [x] 버튼 스타일 통일(글래스로)
- [x] 403 오류
- [x] 서버 삭제
- [x] 서버 떠나기
- [x] 닉네임을 추가하도록.
- [x] 멤버목록 왕관표기 구분 기능 수정
- [x] 서버 떠나기 , 서버 삭제 구분
- [x] 방이름 검색하여 코드로 초대하는 기능(모두 검색 없애고 방 코드로만 추가가능하도록(방 생성시 무작위 여섯자리 알파벳+숫자 조합을 생성)) + 서버 상세페이지에 초대코드 복사버튼같은거 두면 좋을듯
- [x] 커스텀 게임 삭제 시 확인 모달창 → 해당 게임을 등록한 유저가 있는 경우에만
- [x] 모든 시스템 로그 (누군가의 디스코드 로그인, 회원가입, 로그인, 개발자만 볼 수 있도록)
- [x] 랜딩페이지 만들기
- [x] 시간 선택을 06:00, 06:30, 07:00 … 으로
- [x] 친구기능
- [x] 친구가 일정 등록한경우 discord로 알림주기 기능
- [x] 기존 유저가 discord와 연동하는 시스템
- [ ] 친구가 예약되어있는 서버 표기
- [ ] 이메일 인증기능
- [ ] 서버
- [ ] 파티모집 시스템
- [ ] 유저 강퇴하면 스케줄삭제
- [ ] 알림 안지워지는 오류
- [ ] 이미 서버 참가중인 친구는 안뜨게

### 현재 버그
- 열려있는 버그
  - 알림 안지워지는 오류
  - 이미 서버 참가중인 친구는 안뜨게 (목록 필터링 미적용)
- 해결된 버그
  - 403 오류

### 앞으로 할 일 (Backlog)
- 인증: 액세스 토큰 재발급 흐름 보강 및 블랙리스트 전략 재검토
- 문서화: Swagger/OpenAPI 스키마 자동화 및 샘플 요청/응답 추가
- 관찰성: 요청 트레이싱/구분 로그, 에러 응답 규격 표준화
- 성능: 공통 조회에 캐시(예: Redis) 도입, DB 인덱스 점검
- 안정성: 통합 테스트 보강, 스케줄러/서비스 경계 테스트 추가
- 인프라: CI 파이프라인, 프로필별 설정 분리 및 배포 스크립트 템플릿

### 기여 규칙 (Snippet)
새 줄을 추가할 때는 아래 예시처럼 적어 주세요.

```
| 2025-08-14 | your-id | 모듈명 | 한 줄 요약 (PR/이슈 참조) | `상대경로/파일1`, `상대경로/파일2` | 완료 |
```


