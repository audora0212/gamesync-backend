# GameSync 백엔드

## 소개

GameSync는 게임 모임 주최자와 참여자가 안정적으로 서버를 생성 및 관리하고, 게임 세션을 예약·조회하며, 통계를 통해 참여 패턴을 분석할 수 있는 백엔드 애플리케이션입니다. Spring Boot 기반으로 개발되었으며, RESTful API를 제공합니다.

## 주요 기능

- **서버 관리**: 서버 생성, 참가 요청, 이름/초기화 시간 수정, 삭제
- **게임 관리**: 기본 게임 목록 조회, 커스텀 게임 추가 및 삭제
- **타임테이블**: 슬롯별 게임 예약, 중복 처리, 필터·정렬 조회
- **통계**: 서버별 인기 게임, 시간대별 예약 통계
- **인증/인가**: JWT 기반 회원가입·로그인·로그아웃, Discord OAuth2 연동 지원
- **스케줄링**: 만료 토큰 정리, 타임테이블 자동 초기화

## 기술 스택

- **프레임워크**: Spring Boot 3.5.0 (Java 22)
- **데이터베이스**: MySQL (AWS RDS 권장)
- **보안**: Spring Security + JWT, 역할 기반 접근 제어
- **ORM**: Spring Data JPA
- **스케줄링**: `@Scheduled` 사용 (토큰 블랙리스트 정리, 타임테이블 초기화)
- **빌드 도구**: Gradle
- **배포 환경**: AWS EC2 + CodeDeploy
- **로깅**: SLF4J + Logback, AWS CloudWatch

## 요구 사항

- Java 22 이상
- Gradle 7.x
- MySQL 8.x 이상
- Maven/Gradle wrapper 사용 가능

## 설치 및 실행

1. 레포지토리 클론

    ```
    git clone https://github.com/audora0212/gamesync-backend.git
    cd gamesync-backend
    ```

2. 설정 파일 작성
    - `src/main/resources/application.properties` 파일을 생성하여 데이터베이스 및 JWT 설정을 추가

    ```
    spring.datasource.url=jdbc:mysql://<HOST>:<PORT>/<DB_NAME>
    spring.datasource.username=<USERNAME>
    spring.datasource.password=<PASSWORD>
    
    jwt.secret=<YOUR_SECRET_KEY>
    jwt.expiration-ms=3600000
    ```

3. 데이터베이스 초기화

    ```
    ./gradlew flywayMigrate  # Flyway 사용 시
    ```

4. 애플리케이션 실행

    ```
    ./gradlew bootRun
    ```


## API 문서

모든 요청의 기본 URL: `http://<HOST>:<PORT>/api`

### 인증 (Auth)

| 메서드 | 경로 | 요청 바디 | 설명 |
| --- | --- | --- | --- |
| POST | `/auth/signup` | `{ "username": "string", "password": "string" }` | 회원가입 |
| POST | `/auth/login` | `{ "username": "string", "password": "string" }` | 로그인, JWT 발급 |
| POST | `/auth/logout` | ― (Authorization 헤더에 `Bearer <token>`) | 로그아웃 (토큰 블랙리스트) |

### 서버 (Server)

| 메서드 | 경로 | 요청 바디 | 설명 |
| --- | --- | --- | --- |
| GET | `/servers` | ― (인증 필요) | 가입/참가 서버 목록 조회 |
| POST | `/servers` | `{ "name": "string", "resetTime": "HH:mm" }` | 새 서버 생성 |
| GET | `/servers/{id}` | ― (인증 필요) | 서버 상세 정보 조회 |
| POST | `/servers/{id}/join` | ― (인증 필요) | 서버 참가 요청 |
| PUT | `/servers/{id}/name` | `{ "name": "string" }` | 서버 이름 수정 |
| PUT | `/servers/{id}/reset-time` | `{ "resetTime": "HH:mm" }` | 서버 초기화 시간 수정 |
| DELETE | `/servers/{id}` | ― | 서버 삭제 |
| POST | `/servers/{id}/kick` | `{ "userId": <Long> }` | 멤버 강퇴 |
| POST | `/servers/{id}/admins` | `{ "userId": <Long>, "grant": true/false }` | 관리자 권한 부여/해제 |

### 게임 (Game)

| 메서드 | 경로 | 요청 바디 | 설명 |
| --- | --- | --- | --- |
| GET | `/games/default` | ― | 기본 게임 목록 조회 |
| GET | `/servers/{id}/custom-games` | ― (인증 필요) | 커스텀 게임 목록 조회 |
| POST | `/servers/{id}/custom-games` | `{ "name": "string" }` | 커스텀 게임 추가 |
| DELETE | `/servers/{id}/custom-games/{gameId}` | ― | 커스텀 게임 삭제 |

### 타임테이블 (Timetable)

| 메서드 | 경로 | 요청 파라미터/바디 | 설명 |
| --- | --- | --- | --- |
| GET | `/servers/{id}/timetable` | `?game=<name>&sortByGame=<true/false>` | 예약 조회 (필터·정렬 지원) |
| POST | `/servers/{id}/timetable` | `{ "slot": "YYYY-MM-DD'T'HH:mm:ss", "defaultGameId"?: <Long>, "customGameId"?: <Long> }` | 예약 생성 |
| GET | `/servers/{id}/timetable/stats` | ― | 서버 통계 조회 |

## 테스트

- 단위 테스트: `./gradlew test`
- 통합 테스트: Postman 컬렉션 제공 (optional)

## 배포

1. AWS EC2 인스턴스 준비 (Java, Docker 설치)
2. CI/CD: GitHub Actions → AWS CodeDeploy 연동
3. 환경 변수 및 Secret 구성 (RDS, JWT_SECRET)
4. Auto Scaling, Load Balancer 설정