# GameSync Backend

> **협업:**
> **최영찬 (Lead)** – 전체 아키텍처 설계, 인증/보안, 스케줄러, 중요 API 설계 및 구현
> **정승수 (Team)** – 서비스 구현 (GameService, TimetableService), 예외 처리, 테스트, 문서 보강

---

## 목차

1. [소개](#소개)
2. [주요 기능](#주요-기능)
3. [기술 스택](#기술-스택)
4. [아키텍처 &amp; 디렉토리 구조](#아키텍처--디렉토리-구조)
5. [사전 준비](#사전-준비)
6. [환경 설정](#환경-설정)
7. [빌드 &amp; 실행](#빌드--실행)
8. [API 레퍼런스](#api-레퍼런스)
9. [스케줄러 작업](#스케줄러-작업)
10. [테스트](#테스트)
11. [컨트리뷰팅](#컨트리뷰팅)
12. [Authors
    ](#authors)

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

- **프레임워크:** Spring Boot 3.5.0 (Java 22)
- **DB:** MySQL (AWS RDS)
- **인증:** Spring Security, JWT, OAuth2 (Discord)
- **ORM:** Spring Data JPA (Hibernate)
- **스케줄링:** `@Scheduled` (`cron` 및 `fixedRate`)
- **빌드:** Gradle Wrapper
- **배포:** AWS EC2 + CodeDeploy
- **로깅/모니터링:**  AWS CloudWatch

---

## 아키텍처 & 디렉토리 구조

```
src
└─ main
   ├─ java/com/example/scheduler
   │   ├─ config            # 보안·CORS·OAuth 설정
   │   ├─ controller        # REST API 엔드포인트
   │   ├─ dto               # Request/Response 객체
   │   ├─ entity            # JPA 엔터티
   │   ├─ exception         # 전역 예외 처리
   │   ├─ repository        # Spring Data JPA 리포지토리
   │   ├─ scheduler         # 토큰 정리·타임테이블 리셋 작업
   │   ├─ security          # JWT 필터·유틸리티
   │   └─ service           # 비즈니스 로직
   └─ resources
       ├─ application.properties
       └─ logback.xml
```

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

---

## 스케줄러 작업

- **BlacklistCleanupScheduler**

  - Cron: `0 0 3 * * *` (매일 03:00)
  - 설명: 만료된 토큰을 데이터베이스에서 삭제
- **TimetableResetScheduler**

  - Fixed Rate: 60초 (`@Scheduled(fixedRate = 60000)`)
  - 설명: 서버별 `resetTime` 도달 시 해당 서버의 모든 타임테이블 엔트리 초기화

---

## 테스트

```bash
./gradlew test
```

- **JUnit** 기반 단위/통합 테스트
- 주요 서비스 로직 및 컨트롤러 테스트 커버리지 확보

---

## 컨트리뷰팅

1. Fork 프로젝트
2. 새로운 브랜치 생성 (`feature/your-feature`)
3. 커밋 & PR 생성
4. 리뷰 후 메인 브랜치에 머지

---

## Authors

- **최영찬**

  - 전체 아키텍처 및 보안 설계
  - 인증/인가, 스케줄러, 주요 API 엔드포인트 구현
- **정승수**

  - GameService, TimetableService 구현 및 테스트
  - 예외 처리, 문서 보강

---
