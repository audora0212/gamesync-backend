## 운영일지: 감사 로그(details) 필드 확장 및 로깅 격리

- 일시: 2025-09-29 19:33~19:55 KST (발생조치), 2025-09-29 10:33~10:55 UTC
- 변경자: 운영
- 서비스: gamesync-backend

### 변경 배경

- 운영 중 아래 오류 발생으로 요청 롤백 유발:
  - SQL Error 1406 / Data too long for column 'details'
  - 이후 Hibernate AssertionFailure 및 UnexpectedRollbackException 연쇄 발생
- 원인: `audit_log.details` 컬럼 크기 제한(기본 VARCHAR(255) 추정) < 저장 문자열 길이

### 타임라인

- 19:33:23 KST: 대량 FCM 발송 직후 `Data too long for column 'details'` 발생(로그 기준)
- 19:35~19:45 KST: 원인 분석(엔티티/DDL 확인, 호출 경로 식별)
- 19:45~19:50 KST: 코드 조치 적용(`AuditLog`, `AuditService` 수정)
- 19:50~19:55 KST: 로컬 검증 및 운영 일지 작성
- (추가 필요) DB 스키마 변경 `ALTER TABLE` 적용 일정 수립/실행

### 변경 내용

1) 엔티티 스키마 확장

   - 파일: `src/main/java/com/example/scheduler/domain/AuditLog.java`
   - 변경: `details` 필드를 LOB(TEXT)로 매핑
     - 추가 애너테이션: `@Lob`, `@Column(columnDefinition = "TEXT")`
2) 감사 로그 저장 로직 격리 및 안정화

   - 파일: `src/main/java/com/example/scheduler/service/AuditService.java`
   - 변경:
     - `log(...)` 메서드에 `@Transactional(propagation = REQUIRES_NEW)` 적용 (별도 트랜잭션)
     - `details`를 최대 2000자로 절단 저장(안전 길이)
     - 실패 시 현 트랜잭션만 롤백하고 경고 로그 남김(호출자 트랜잭션 영향 최소화)

### 해결 방식(상세)

- 스키마 레벨: `TEXT`는 가변 길이 저장이므로 실제 데이터만큼 공간 사용. 인덱스 비적용 컬럼으로 성능/용량 영향 최소화
- 애플리케이션 레벨:
  - 트랜잭션 격리: 감사 로그 저장을 호출 트랜잭션과 분리(REQUIRES_NEW)하여 본 업무 로직의 커밋/롤백과 독립
  - 데이터 가드: `MAX_DETAILS_LENGTH = 2000` 상한으로 과도한 문자열 저장 방지(필요 시 조정 가능)
  - 실패 내성: 감사 로그 저장 예외 발생 시 감사 트랜잭션만 롤백, 경고 로그만 남기고 호출자 흐름 유지

### DB 변경(수행 필요)

운영 DB 스키마를 엔티티와 일치시키기 위해 아래 쿼리 실행:

```sql
ALTER TABLE audit_log MODIFY COLUMN details TEXT;
```

권장: 마이그레이션 도구(Flyway/Liquibase)로 배포 관리

### 예상 영향 및 리스크

- `TEXT`는 가변 길이 저장으로 실제 저장 데이터만큼만 공간 사용. 인덱스 없음 → 인덱스 용량 영향 없음
- `REQUIRES_NEW`로 감사 로그 저장 실패가 본 요청 롤백을 유발하지 않음
- 문자열 2000자 절단으로 과도한 로그 폭증 방지. 필요 시 상한 조정 가능

### 배포/적용 순서

1) 애플리케이션 배포 (엔티티/서비스 변경 반영)
2) DB 스키마 변경(`ALTER TABLE`) 적용

비고: 두 단계 어느 순서여도 치명적 이슈는 없으나, 기존 스키마(VARCHAR(255)) 상태에서 긴 `details` 저장 시 잘림 오류가 재현될 수 있으므로 DB 변경을 가급적 선행 권장

### 검증 방법

- 알림/파티/스케줄 생성 등 `auditService.log` 호출 경로 수행
- DB `audit_log.details`에 255자 초과 문자열 정상 저장 확인
- 애플리케이션 로그에 더 이상 `Data too long for column 'details'` 미발생 확인

### 롤백 계획

- 코드 롤백: 이전 버전으로 배포
- DB 롤백: 필요 시 `ALTER TABLE audit_log MODIFY COLUMN details VARCHAR(255);` (단, 기존 TEXT 데이터 손실 가능성 검토 필수)

### 관련 로그 예시

- `SQL Error: 1406, Data too long for column 'details' at row 1`
- `UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only`
