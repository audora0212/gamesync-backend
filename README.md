# GameSync Backend

> **최영찬 (Lead)** – 전체 아키텍처 설계, 인증/보안, 스케줄러, 중요 API 설계 및 구현
>
> **정승수 (Team)** – 서비스 구현 (GameService, TimetableService), 예외 처리, 테스트, 문서 보강

---

## 목차

1. [만든 계기](#만든-계기)
2. [소개](#소개)
3. [주요 기능](#주요-기능)
4. [기술 스택](#기술-스택)
5. [왜 이런 선택을 했나 (ADR 요약)](#왜-이런-선택을-했나-adr-요약)
6. [핵심 API 사용 예시](#핵심-api-사용-예시)
7. [아키텍처 &amp; 디렉토리 구조](#아키텍처--디렉토리-구조)
8. [사전 준비](#사전-준비)
9. [환경 설정](#환경-설정)
10. [빌드 &amp; 실행](#빌드--실행)
11. [API 레퍼런스](#api-레퍼런스)
12. [스케줄러 작업](#스케줄러-작업)
13. [CI/CD (추가 예정)](#cicd-추가-예정)
14. [테스트](#테스트)
15. [컨트리뷰팅](#컨트리뷰팅)
16. [Authors
    ](#authors)

---

## 만든 계기

친구들이랑 밤마다 디스코드에 모여 “오늘 몇 시에 시작할까?”, “이번엔 무슨 게임 하지?”를 매번 처음부터 다시 정하는 게 점점 귀찮아졌습니다. 공지 채널은 금방 묻히고, 엑셀로 정리하자니 계속 수정하다가 다들 포기하곤 했죠. 한 번은 알림을 놓쳐서 절반만 접속하고, 또 한 번은 서로 시간이 안 맞아 예약을 겹치게 잡는 바람에 허탈했던 적도 있었어요.

그래서 아예 ‘우리 모임’에 맞는 아주 작은 도구를 만들기 시작했습니다. 서버(길드)마다 게임을 관리하고, 일정을 예약하면, 시작 전에 조용히 푸시 알림으로 챙겨주는 것. 로그인은 간단하고, 초대나 합류는 번거롭지 않게, 무엇보다 누구 하나가 계속 떠밀지 않아도 약속이 돌아가도록요.

- 우리가 중요하게 본 것
  - 간단한 로그인과 합류: OAuth, 초대/참여 흐름 최소 마찰
  - 잊지 않게, 시끄럽지 않게: 시작 전 푸시 리마인더, 불필요한 저장형 알림 최소화
  - 모임답게: 서버별 게임/멤버/권한 관리, 가벼운 파티 기능
  - 믿음직하게: JWT, 토큰 블랙리스트, 최소 권한의 보안 구성

---

## 소개

GameSync 백엔드는 게임 모임 주최자와 참여자 간에

- **서버 생성·관리**
- **게임 세션 예약·조회·통계**
- **JWT 기반 인증 + Discord OAuth2 연동**
- **스케줄러를 이용한 자동 정리 작업**

등의 기능을 제공하는 Spring Boot 기반 마이크로서비스입니다.

---

## 주요 기능

- **인증 & 권한 관리**
  - JWT 발급/검증, 토큰 블랙리스트 처리
  - Discord OAuth2 로그인
- **서버 관리**
  - 서버 생성·삭제·수정
  - 멤버 초대·강퇴, 관리자 권한 부여
- **게임 관리**
  - 기본(Default) 게임 목록 조회
  - 서버별 커스텀 게임 CRUD
- **타임테이블 예약**
  - 중복 슬롯 자동 처리
  - 게임별/날짜별 필터·정렬
  - 서버별 예약 통계 조회
- **스케줄러**
  - 만료 토큰 정리
  - 타임테이블 엔트리 자동 리셋

---

## 기술 스택

- **프레임워크:** Spring Boot 3.5.0 (Java 17)
- **DB:** MySQL (AWS RDS)
- **인증:** Spring Security, JWT, OAuth2 (Discord, Kakao)
- **ORM:** Spring Data JPA (Hibernate)
- **스케줄링:** `@Scheduled` (`cron` 및 `fixedRate`)
- **빌드:** Gradle Wrapper
- **배포:** AWS EC2 + CodeDeploy
- **로깅/모니터링:**  AWS CloudWatch

---

## 왜 이런 선택을 했나 (ADR 요약)

- **인증: JWT + 토큰 블랙리스트**

  - **대안**: 서버 세션, Redis 세션 스토어
  - **선택 근거**: 다양한 클라이언트(웹/PWA) 환경에서 무상태 특성을 유지해 확장성과 배포 단순성을 확보하기 위함. 프록시/로드밸런서 뒤에서도 동작이 일관적임.
  - **운영 포인트**: 로그아웃·탈취 대응을 위해 `BlacklistedToken`을 저장하고, 스케줄러로 만료 정리를 수행. 키 관리와 토큰 만료 정책을 명확히 문서화.
- **예약 중복 방지: RDB 제약 + 트랜잭션**

  - **대안**: 애플리케이션 레벨 락, 분산 락(Redis)
  - **선택 근거**: 예약의 유일성은 데이터 계층에서 강제하는 것이 가장 신뢰도 높음. UNIQUE 제약/조건부 삽입으로 경쟁 상태를 스키마 차원에서 차단.
  - **운영 포인트**: 사용자 친화적 오류 메시지로 매핑, 인덱스 설계와 쿼리 튜닝으로 성능 확보, 에러 코드 규격화.
- **리마인더/리셋: Spring `@Scheduled` 기반**

  - **대안**: 메시지 큐(Kafka/SQS) + 워커 아키텍처
  - **선택 근거**: 초기 규모와 팀 역량을 고려해 운영 복잡도를 낮추는 접근을 우선. 요구가 증가하면 큐 기반으로 자연스럽게 전환 가능.
  - **운영 포인트**: 지연/드리프트 모니터링 지표 정의, 스케줄러 인터페이스 분리로 교체 용이성 확보.
- **인프라: ALB + EC2 + RDS**

  - **대안**: API Gateway + Lambda, ECS/Fargate, App Runner
  - **선택 근거**: 런타임/OS 제어가 가능해 실습·데모 환경에서 학습 가치가 높고, 비용·복잡도 균형이 양호함.
  - **운영 포인트**: 헬스체크/롤백 정책 수립, ASG 확장 전략, 무중단 배포 절차 마련.
- **네트워킹: 프라이빗 서브넷 + NAT egress**

  - **대안**: 퍼블릭 EC2 + 보안그룹 제한
  - **선택 근거**: 데이터 계층 접근 리소스를 비공개화하고, 외부 서비스(FCM) 호출은 NAT→IGW 경로로 최소 노출.
  - **운영 포인트**: AZ별 NAT 가용성/비용 고려, 라우팅 테이블·보안그룹 일관성 점검.
- **데이터베이스: MySQL (RDS)**

  - **대안**: PostgreSQL, DynamoDB
  - **선택 근거**: 트랜잭션/조인 지원과 성숙한 생태계, 팀 숙련도.
  - **운영 포인트**: 마이그레이션(Flyway) 도입 여지, 인덱스/쿼리 최적화 계획 수립.

---

## 핵심 API 사용 예시

- **인증 토큰 발급**

```bash
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"demo@example.com","password":"pass1234"}' \
  http://localhost:8080/api/auth/login
```

응답 예시:

```json
{
  "token": "<JWT>",
  "message": "로그인 성공"
}
```

- **서버 생성**

```bash
curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{"name":"우리길드","resetTime":"03:00"}' \
  http://localhost:8080/api/servers
```

응답 예시:

```json
{
  "id": 123,
  "name": "우리길드",
  "resetTime": "03:00"
}
```

- **타임테이블 예약 등록**

```bash
curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{"slot":"2025-08-20T21:00:00Z","defaultGameId":1}' \
  http://localhost:8080/api/servers/123/timetable
```

- **친구 요청 보내기**

```bash
curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{"friendCode":"ABCD-1234"}' \
  http://localhost:8080/api/friends/request
```

- **권한/오류 예시 (401 Unauthorized)**

```bash
curl -i http://localhost:8080/api/servers
```

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer
Content-Type: application/json

{"error":"UNAUTHORIZED","message":"Authentication is required"}
```

---

## 아키텍처 & 디렉토리 구조

```
src
└─ main
   ├─ java/com/example/scheduler
   │   ├─ config            # 초기화, Firebase 등 구성
   │   ├─ controller        # REST API 엔드포인트
   │   ├─ dto               # Request/Response 객체
   │   ├─ domain            # JPA 엔터티
   │   ├─ repository        # Spring Data JPA 리포지토리
   │   ├─ scheduler         # 토큰 정리·타임테이블 리셋·리마인더
   │   ├─ security          # JWT 필터·OAuth2 핸들러
   │   └─ service           # 비즈니스 로직
   └─ resources
       └─ default_games.txt
```

---

## AWS 구성도(수정)

![AWS 구성도(수정)](./aws구성도%28수정%29.png)

## ERD

![ERD](./erd.png)

---

## 사전 준비

- **Java 22** 이상 설치
- **MySQL 8** 이상 인스턴스 (AWS RDS 권장)
- **Gradle Wrapper** (프로젝트에 포함)

---

## 환경 설정

`src/main/resources/application.properties` 또는 환경 변수:

```properties
# 서버 포트
server.port=8080

# 데이터베이스
spring.datasource.url=jdbc:mysql://<HOST>:3306/<DB_NAME>?useSSL=false&serverTimezone=UTC
spring.datasource.username=<DB_USER>
spring.datasource.password=<DB_PASSWORD>

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=<YOUR_JWT_SECRET>
jwt.expiration-ms=3600000

# Discord OAuth2
spring.security.oauth2.client.registration.discord.client-id=<CLIENT_ID>
spring.security.oauth2.client.registration.discord.client-secret=<CLIENT_SECRET>
spring.security.oauth2.client.registration.discord.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.discord.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.discord.scope=identify,email
spring.security.oauth2.client.provider.discord.authorization-uri=https://discord.com/api/oauth2/authorize
spring.security.oauth2.client.provider.discord.token-uri=https://discord.com/api/oauth2/token
spring.security.oauth2.client.provider.discord.user-info-uri=https://discord.com/api/users/@me
spring.security.oauth2.client.provider.discord.user-name-attribute=id
 
# Kakao OAuth2
spring.security.oauth2.client.registration.kakao.client-id=<CLIENT_ID>
spring.security.oauth2.client.registration.kakao.client-secret=<CLIENT_SECRET>
spring.security.oauth2.client.registration.kakao.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.kakao.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.kakao.scope=profile_nickname,account_email
spring.security.oauth2.client.provider.kakao.authorization-uri=https://kauth.kakao.com/oauth/authorize
spring.security.oauth2.client.provider.kakao.token-uri=https://kauth.kakao.com/oauth/token
spring.security.oauth2.client.provider.kakao.user-info-uri=https://kapi.kakao.com/v2/user/me
spring.security.oauth2.client.provider.kakao.user-name-attribute=id
 
# Firebase Admin (권장: 환경 변수 사용)
# GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
```

---

## 빌드 & 실행

```bash
# 프로젝트 루트에서
./gradlew clean build

# JAR 실행
java -jar build/libs/gamesync-backend-0.0.1-SNAPSHOT.jar

# 개발 모드
./gradlew bootRun
```

Windows(PowerShell/명령 프롬프트):

```bat
gradlew.bat clean build
java -jar build\libs\gamesync-backend-0.0.1-SNAPSHOT.jar
gradlew.bat bootRun
```

---

## API 레퍼런스

### 인증

| 메서드 | 경로                 | 설명                                                     |
| ------ | -------------------- | -------------------------------------------------------- |
| POST   | `/api/auth/signup` | `{ username, password }` → `{ message }`            |
| POST   | `/api/auth/login`  | `{ username, password }` → `{ token, message }`     |
| POST   | `/api/auth/logout` | 헤더 `Authorization: Bearer <token>` (블랙리스트 추가) |

### 서버

| 메서드 | 경로                             | 설명                                      |
| ------ | -------------------------------- | ----------------------------------------- |
| GET    | `/api/servers`                 | 가입/참가 가능 서버 목록 조회             |
| POST   | `/api/servers`                 | `{ name, resetTime }` → 서버 생성      |
| GET    | `/api/servers/{id}`            | 서버 상세 조회                            |
| POST   | `/api/servers/{id}/join`       | 서버 참가                                 |
| PUT    | `/api/servers/{id}/name`       | `{ name }` → 서버 이름 변경            |
| PUT    | `/api/servers/{id}/reset-time` | `{ resetTime }` → 초기화 시간 변경     |
| DELETE | `/api/servers/{id}`            | 서버 삭제                                 |
| POST   | `/api/servers/{id}/kick`       | `{ userId }` → 멤버 강퇴               |
| POST   | `/api/servers/{id}/admins`     | `{ userId, grant }` → 관리자 임명/해제 |

### 즐겨찾기

| 메서드 | 경로                                  | 설명          |
| ------ | ------------------------------------- | ------------- |
| GET    | `/api/servers/favorites`            | 즐겨찾기 목록 |
| POST   | `/api/servers/{id}/favorite/toggle` | 즐겨찾기 토글 |

### 게임

| 메서드 | 경로                                              | 설명                             |
| ------ | ------------------------------------------------- | -------------------------------- |
| GET    | `/api/games/default`                            | 기본 게임 목록 조회              |
| GET    | `/api/servers/{id}/custom-games`                | 커스텀 게임 목록 조회            |
| POST   | `/api/servers/{id}/custom-games`                | `{ name }` → 커스텀 게임 추가 |
| DELETE | `/api/servers/{id}/custom-games/{customGameId}` | 커스텀 게임 삭제                 |

### 타임테이블

| 메서드 | 경로                                  | 설명                                                     |
| ------ | ------------------------------------- | -------------------------------------------------------- |
| GET    | `/api/servers/{id}/timetable`       | 예약 목록 (게임/정렬 필터 지원)                          |
| POST   | `/api/servers/{id}/timetable`       | `{ slot, defaultGameId?, customGameId? }` → 예약 등록 |
| GET    | `/api/servers/{id}/timetable/stats` | 서버별 예약 통계 조회                                    |

### 친구

| 메서드 | 경로                            | 설명      |
| ------ | ------------------------------- | --------- |
| GET    | `/api/friends`                | 친구 목록 |
| POST   | `/api/friends/request`        | 친구 요청 |
| POST   | `/api/friends/accept`         | 친구 수락 |
| POST   | `/api/friends/reject`         | 친구 거절 |
| DELETE | `/api/friends/{friendUserId}` | 친구 삭제 |

### 파티

| 메서드 | 경로                             | 설명      |
| ------ | -------------------------------- | --------- |
| GET    | `/api/servers/{id}/parties`    | 파티 목록 |
| POST   | `/api/servers/{id}/parties`    | 파티 생성 |
| POST   | `/api/parties/{partyId}/join`  | 파티 참가 |
| POST   | `/api/parties/{partyId}/leave` | 파티 탈퇴 |
| DELETE | `/api/parties/{partyId}`       | 파티 삭제 |

---

## 스케줄러 작업

- **BlacklistCleanupScheduler**

  - Cron: `0 0 3 * * *` (매일 03:00)
  - 설명: 만료된 토큰을 데이터베이스에서 삭제
- **TimetableResetScheduler**

  - Fixed Rate: 60초 (`@Scheduled(fixedRate = 60000)`)
  - 설명: 서버별 `resetTime` 도달 시 해당 서버의 모든 타임테이블 엔트리 초기화
- **TimetableReminderScheduler**

  - Fixed Rate: 60초
  - 설명: 사용자 설정된 N분 전 합류시간 푸시 리마인더 발송(저장형 알림 미생성)

---

## CI/CD (추가 예정)

- **GitHub Actions**

  - Gradle 캐시 + 빌드/테스트, 아티팩트(S3 업로드)
  - OIDC로 AWS AssumeRole (IAM) → 자격증명 없이 안전하게 배포
  - 브랜치/태그 전략에 따라 배포 트리거 분기
- **CodeDeploy Blue/Green + ASG**

  - ALB 2개 Target Group 간 트래픽 스위치, 헬스체크 실패 시 자동 롤백
  - Auto Scaling Group 기반 다중 AZ, 배포 중 용량/트래픽 분할 설정
  - AppSpec 훅: BeforeInstall/AfterInstall/ApplicationStart/ValidateService
- **필요 리소스/시크릿(요약)**

  - S3 버킷(아티팩트), CodeDeploy App/DeploymentGroup(Blue/Green)
  - ALB + Target Groups, ASG + Launch Template, IAM 역할(액션스 OIDC)
  - Repo secrets: `AWS_REGION`, `AWS_ROLE_ARN`, `S3_BUCKET`, `CODEDEPLOY_APP`, `CODEDEPLOY_GROUP`
- **추가 계획**

  - `.github/workflows/deploy.yml`, `appspec.yml`, 배포 스크립트 및 헬스체크 문서화 예정

---

## 테스트

```bash
./gradlew test
```

- **JUnit** 기반 통합 테스트


## Authors

- **최영찬**

  - 전체 아키텍처 및 보안 설계
  - 인증/인가, 스케줄러, 주요 API 엔드포인트 구현
- **정승수**

  - GameService, TimetableService 구현 및 테스트
  - 예외 처리, 문서 보강

---
