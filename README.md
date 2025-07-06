## Gameslot 프로젝트 기획

---

### 1. 프로젝트 개요

- **프로젝트명**: Gameslot
- **목적**: 게임 모임 주최자와 참여자가 안정적으로 서버를 만들고, 게임 세션을 예약·관리하며, 통계를 통해 패턴을 분석할 수 있는 웹 애플리케이션
- **주요 사용자**: 게임 커뮤니티 리더, 정기 모임 진행자, 게이머

---

## 2. 백엔드 상세 설계

### 2.1 기술 스택

- **프레임워크**: Spring Boot 3.5.0 (Java 22)
- **데이터베이스**: MySQL (AWS RDS)
- **인증**: JWT + Spring Security
- **스케줄링**: `@Scheduled` cron 기반 작업 (토큰 블랙리스트 정리, 타임테이블 초기화)
- **ORM**: Spring Data JPA
- **빌드/배포**: Maven, AWS EC2 + CodeDeploy
- **로깅/모니터링**: SLF4J + Logback, CloudWatch Metrics

### 2.2 주요 도메인 & 서비스 구조

- **AuthService**: 회원가입, 로그인, 로그아웃(토큰 블랙리스트 저장)
- **ServerService**: 서버 생성/참가/수정/삭제, 권한 체크(Owner/Admin)
- **GameService**: 기본 게임·커스텀 게임 CRUD, 권한 검증
- **TimetableService**: 예약 추가(중복 자동 처리), 조회(필터·정렬), 통계 산출
- **Scheduler**:
    - `BlacklistCleanupScheduler`: 매일 03:00 만료 토큰 삭제
    - `TimetableResetScheduler`: 서버별 resetTime 매 분 검사 후 엔트리 삭제

### 2.3 예외 처리 & 보안

- 전역 `@ControllerAdvice`로 `ResponseStatusException` 변환
- 역할 기반 접근 제어: owner/admin 여부 확인 후 `403` 처리
- CORS/CSP 설정: 서버 간 보안 강화
- 입력값 검증: DTO에 `@Valid` + Custom Validator

### 2.4 데이터 모델 (ERD 요약)

- **User**(id, username, password)
- **Server**(id, name, reset_time, owner_id, members, admins)
- **DefaultGame**, **CustomGame**(id, name, server_id)
- **TimetableEntry**(id, server_id, user_id, slot, default_game_id?, custom_game_id?)
- **BlacklistedToken**(id, token, expiry)

### 2.5 API 명세

| 엔드포인트 | 메서드 | 요청/응답 주요 필드 | 설명 |
| --- | --- | --- | --- |
| `/api/auth/signup` | POST | `{username, password}` → `{message}` | 회원가입 |
| `/api/auth/login` | POST | `{username, password}` → `{token, message}` | 로그인 및 JWT 발급 |
| `/api/auth/logout` | POST | 헤더 `Authorization: Bearer <token>` | 토큰 블랙리스트 추가 |
| `/api/servers` | GET | 헤더 인증 → `List<ServerResponse>` | 가입 가능한 서버/참가한 서버 목록 |
| `/api/servers` | POST | `{name, resetTime}` → `ServerResponse` | 새 서버 생성 |
| `/api/servers/{id}` | GET | PathParam id → `ServerResponse` | 서버 상세 (개요, 멤버, 관리권한) |
| `/api/servers/{id}/join` | POST | PathParam id → `ServerResponse` | 서버 참가 요청 |
| `/api/servers/{id}/name` | PUT | `{name}` → `ServerResponse` | 서버 이름 변경 |
| `/api/servers/{id}/reset-time` | PUT | `{resetTime}` → `ServerResponse` | 초기화 시간 수정 |
| `/api/servers/{id}` | DELETE | — → `204 No Content` | 서버 삭제 |
| `/api/servers/{id}/kick` | POST | `{userId}` → `ServerResponse` | 멤버 강퇴 |
| `/api/servers/{id}/admins` | POST | `{userId, grant}` → `ServerResponse` | 관리자 임명/해제 |
| `/api/games/default` | GET | — → `DefaultGameListResponse` | 기본 게임 목록 조회 |
| `/api/servers/{id}/custom-games` | GET | — → `CustomGameListResponse` | 커스텀 게임 목록 조회 |
| `/api/servers/{id}/custom-games` | POST | `{name}` → `CustomGameResponse` | 커스텀 게임 추가 |
| `/api/servers/{id}/custom-games/{gid}` | DELETE | — → `204 No Content` | 커스텀 게임 삭제 |
| `/api/servers/{id}/timetable` | GET | `?game=&sortByGame=` → `List<EntryResponse>` | 타임테이블 조회 (필터·정렬 포함) |
| `/api/servers/{id}/timetable` | POST | `{slot, defaultGameId?, customGameId?}` → `EntryResponse` | 타임테이블 예약 |
| `/api/servers/{id}/timetable/stats` | GET | — → `StatsResponse` | 서버 통계 |

---

## 3. 프론트엔드 상세 설계

### 3.1 기술 스택

- **프레임워크**: Next.js 14 (App Router), React 18
- **스타일링**: Tailwind CSS + Glassmorphism 확장 (tailwindcss-animate)
- **컴포넌트**: shadcn/ui, lucide-react 아이콘, Sonner Toast
- **상태 관리**: React Context (`AuthProvider`), useState/useEffect
- **빌드/배포**: Vercel, GitHub Actions CI

### 3.2 페이지 및 컴포넌트 구조

```
/pages
 ├─ /auth
 │    ├─ login/page.tsx
 │    └─ signup/page.tsx
 ├─ /dashboard/page.tsx
 ├─ /server/[id]/page.tsx
 │    ├─ GameManagement
 │    ├─ TimetableView
 │    └─ ServerOverview
 └─ /stats/[id]/page.tsx
/components
 ├─ auth-provider.tsx
 ├─ navbar.tsx
 ├─ create-server-modal.tsx
 ├─ game-management.tsx
 ├─ timetable-view.tsx
 └─ server-overview.tsx

```

### 3.3 주요 컴포넌트 설명

- **AuthProvider**: 인증 상태 확인, 라우팅 가드
- **Navbar**: 공통 상단바, 라우팅·로그아웃
- **DashboardPage**: 서버 카드 목록, 생성 모달, 참가 기능
- **ServerDetailPage**: 3-컬럼 레이아웃
    - `GameManagement`: 기본·커스텀 게임 관리
    - `TimetableView`: 예약 폼, 필터·정렬, 리스트 렌더링
    - `ServerOverview`: 개요, 멤버 목록, 권한 표시
- **StatsPage**: 카드형 통계 UI (인기 게임, 시간대, 참여자)

### 3.4 UX 흐름

1. **로그인/회원가입** → `AuthProvider`에서 토큰 확인 후 리다이렉트
2. **대시보드**: 서버 목록 로드 → 카드 클릭 시 상세 페이지
3. **상세 페이지**:
    - 좌측: 게임 추가/삭제
    - 중앙: 날짜·시간·게임 선택 → 예약 등록 → 리스트 갱신
    - 우측: 서버 정보 수정, 멤버 관리, 통계 페이지 이동
4. **통계 페이지**: 실시간 데이터 조회 및 카드 렌더링

---

## 4. 배포 전략

- **백엔드**: AWS EC2 + RDS(MySQL) + ASG(Auto Scaling Group)
- **프론트엔드**: AWS EC2
- **CI/CD**: GitHub Actions → AWS CodeDeploy
- **모니터링**: CloudWatch Logs,

---
