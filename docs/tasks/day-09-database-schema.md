# Task: Day 9 PostgreSQL 스키마와 Flyway migration

## 목적

Day 8의 `Event`, `Seat`, `Reservation` JPA 도메인을 명시적인 PostgreSQL 스키마로
고정하고, 이후 schema 변경을 Flyway의 버전 관리 migration으로 수행한다. 운영 및
로컬 PostgreSQL에서는 Hibernate가 schema를 변경하지 않고 애플리케이션 시작 시
Entity와 실제 schema의 일치 여부만 검증하게 한다.

## 작업 범위

- Spring Boot 4.1.0과 PostgreSQL에 맞는 Flyway core 및 PostgreSQL 지원 모듈을
  추가한다.
- 최초 migration `V1__create_ticketing_schema.sql`에 `events`, `seats`,
  `reservations` 테이블과 현재 Entity 관계를 명시한다.
- Entity의 필수 필드와 동일하게 기본키, 외래키, null 허용 여부 및 좌석 상태 값을
  제약조건으로 표현한다.
- 이벤트 안에서 좌석 번호가 중복되지 않게 `(event_id, seat_number)` unique
  constraint를 둔다. 이 unique index는 이벤트별 좌석 번호순 조회도 지원하므로 별도
  중복 index를 만들지 않는다.
- 좌석당 예약을 하나만 허용하도록 `reservations.seat_id`에 unique constraint를
  둔다. 이 unique index가 좌석 외래키 탐색도 지원하므로 별도 index를 만들지 않는다.
- 기본키를 이용하는 이벤트 단건 조회와 좌석 ID 조회에는 별도 index를 추가하지
  않는다.
- 운영 및 로컬 PostgreSQL의 `ddl-auto`를 `validate`로 바꾸고 schema 생성과 변경은
  Flyway가 담당하게 한다.
- 기존 H2 기반 API 테스트는 빠른 격리 목적의 `create-drop`을 유지하고 Flyway를
  비활성화한다.
- Testcontainers PostgreSQL 통합 테스트로 빈 DB migration, Flyway history,
  Hibernate validation, 애플리케이션 시작, 기존 API 흐름 및 재시작 시 migration
  비중복 적용을 검증한다.
- 별도 Compose 프로젝트와 disposable volume을 사용하는 smoke test에서 실제 이미지
  시작, health, 티켓팅 API 및 재시작 동작을 검증한다.
- 기존 `ddl-auto=update` volume의 일회성 초기화 절차와 데이터 손실 가능성을
  문서화한다.

## 제외 범위

- 동시 예약 locking, 비관적 락, 낙관적 락 및 재시도 전략
- Redis, cache, Kafka 및 대기열
- 인증·권한, 결제 및 예약 취소
- Predictor 연동, 부하 테스트, Kubernetes 및 observability
- 새로운 비즈니스 API
- Flyway baseline 자동화 및 `baseline-on-migrate=true`
- 기존 PostgreSQL volume의 자동 삭제, 초기화 또는 데이터 변환
- 불필요한 공통 계층, 범용 schema 추상화 및 관련 없는 리팩터링
- Velog 글

## Acceptance Criteria

- [x] Flyway 의존성과 PostgreSQL 지원 모듈이 추가되었다.
- [x] 최초 migration이 빈 PostgreSQL DB에 세 도메인 테이블을 생성한다.
- [x] 기본키, 외래키, null 제약, 좌석 상태 check 및 필요한 unique constraint가
      Entity와 비즈니스 규칙에 맞는다.
- [x] 현재 Repository 조회를 지원하는 최소 index만 존재한다.
- [x] 운영 및 로컬 PostgreSQL에서 Hibernate `ddl-auto=update`가 제거되고
      `validate`가 적용된다.
- [x] H2 기반 기존 테스트의 독립성과 실행 속도가 유지된다.
- [x] Testcontainers에서 Flyway schema history와 migration 성공이 검증된다.
- [x] PostgreSQL schema에 대한 Hibernate validation과 애플리케이션 시작이 성공한다.
- [x] PostgreSQL에서 기존 Event, Seat, Reservation API 흐름이 유지된다.
- [x] 애플리케이션 재시작 후 V1 migration이 중복 적용되지 않는다.
- [x] 기존 로컬 DB를 위한 명시적 일회성 초기화 방법과 데이터 손실 위험이 기록된다.
- [x] `ticketing-api` 테스트, 전체 검증 스크립트 및 가능한 Compose smoke test가
      통과한다.
- [x] Day 9 회고에 실제 검증 결과와 남은 위험이 기록된다.
- [x] Day 9 제외 범위와 무관한 파일을 변경하지 않았다.

## 검증 명령

```bash
cd ticketing-api
./gradlew test

cd ..
./scripts/verify.sh
./scripts/smoke-test.sh
git diff --check
```

`PostgresMigrationIntegrationTests`는 Docker의 disposable PostgreSQL container를
사용한다. Docker를 사용할 수 없는 환경에서는 해당 사실과 실행하지 못한 검증을 완료
결과와 회고에 명확히 기록하며, H2 결과로 PostgreSQL 검증을 대체했다고 표현하지
않는다.

## 예상 변경 파일

- `docs/tasks/day-09-database-schema.md`
- `docs/retrospectives/day-09.md`
- `ticketing-api/build.gradle`
- `ticketing-api/src/main/java/com/eschoi04/ticketing_api/event/Event.java`
- `ticketing-api/src/main/resources/application.properties`
- `ticketing-api/src/main/resources/db/migration/V1__create_ticketing_schema.sql`
- `ticketing-api/src/test/resources/application.properties`
- `ticketing-api/src/test/java/com/eschoi04/ticketing_api/PostgresMigrationIntegrationTests.java`
- `scripts/smoke-test.sh`

## 위험 요소

- Day 8의 Hibernate `update`로 생성된 기존 schema에는 Flyway history가 없다. Flyway는
  비어 있지 않은 무이력 schema를 자동 소유하지 않으므로 기존 개발 DB는 명시적으로
  초기화해야 한다.
- 초기화는 기존 로컬 데이터를 삭제한다. 보존해야 할 데이터가 있다면 먼저 dump하고
  별도의 데이터 이관 migration을 설계해야 하며 이번 범위에서는 자동화하지 않는다.
- H2는 PostgreSQL SQL과 제약 동작을 완전히 재현하지 못한다. 따라서 빠른 API
  테스트와 별도로 실제 PostgreSQL Testcontainers 검증을 둔다.
- Docker daemon을 사용할 수 없는 실행 환경에서는 PostgreSQL 통합 테스트와 Compose
  smoke test를 수행할 수 없다. 이 경우 미검증 범위를 숨기지 않는다.
- unique constraint는 최종 중복 예약 행을 막지만 동시 요청의 HTTP 결과와 좌석 상태
  경쟁을 해결하지 않는다. locking 전략은 후속 task 범위다.

## 완료 결과

- Spring Boot 4.1의 Flyway 자동설정을 제공하는 `spring-boot-starter-flyway`와
  PostgreSQL 지원 모듈을 추가했다. Testcontainers 2.x의 JUnit Jupiter 및 PostgreSQL
  모듈도 테스트 범위에 추가했다.
- V1은 `events`, `seats`, `reservations`를 identity 기본키로 만들고 두 외래키,
  필수 열의 `NOT NULL`, 좌석 상태 check, 이벤트별 좌석 번호 unique 및 좌석별 예약
  unique를 정의한다. 두 unique constraint의 index가 현재 Repository 조회를
  지원하므로 별도 index는 추가하지 않았다.
- 운영·Compose 설정은 `ddl-auto=validate`로 전환했다. 테스트 설정은 H2
  `create-drop`을 유지하고 Flyway를 끈다. 별도 PostgreSQL 통합 테스트만 Flyway와
  `validate`를 명시적으로 켠다.
- `PostgresMigrationIntegrationTests`는 PostgreSQL 17 disposable container의 빈 DB에
  애플리케이션을 시작하고 V1 history 1건, Hibernate validation, 이벤트·좌석 생성과
  예약 API를 확인한다. 같은 DB로 애플리케이션을 재시작해 V1이 1건인 상태와 기존
  이벤트 데이터가 유지되는지도 확인한다.
- `./gradlew test`가 기존 H2 테스트 8개와 PostgreSQL migration 테스트 1개를 포함해
  통과했다. `./scripts/verify.sh`와 보강한 `./scripts/smoke-test.sh`도 통과했다.
  Compose에서는 세 컨테이너 health, Flyway history, API 예약 흐름, 재시작 후 V1
  1건 및 PostgreSQL 장애 시 health `DOWN`을 확인했다.

### 기존 로컬 DB 일회성 전환

Day 8의 `ddl-auto=update`로 만든 비어 있지 않은 DB에는 `flyway_schema_history`가
없다. 이 상태에서 Day 9 애플리케이션을 시작하면 Flyway가 migration을 소유할 수 없어
시작에 실패하는 것이 정상이다. `baseline-on-migrate`로 기존 구조를 신뢰하지 않는다.

기존 로컬 데이터가 필요하면 먼저 별도 파일로 dump하고, 자동 초기화 대신 데이터 이관
migration을 설계해야 한다. 이번 Day 9 범위에는 기존 데이터 이관이 포함되지 않는다.

데이터를 버려도 되는 초기 개발용 Compose DB라면 다음 일회성 절차를 사용한다.

```bash
# 주의: 첫 명령은 현재 Compose PostgreSQL 데이터를 백업한다.
docker compose exec --no-TTY postgres pg_dump \
  --username flashscale --dbname flashscale --format=custom \
  --file=/tmp/flashscale-before-flyway.dump
docker compose cp postgres:/tmp/flashscale-before-flyway.dump \
  ./flashscale-before-flyway.dump

# 주의: --volumes는 이 Compose 프로젝트의 기존 PostgreSQL 데이터를 삭제한다.
docker compose down --volumes
docker compose up --build --detach --wait
```

백업 파일은 저장소에 커밋하지 않는다. `down --volumes` 전에는 대상 Compose project와
데이터 삭제 동의를 직접 확인해야 한다. 이번 자동 검증에서는 이 명령을 사용자의 기존
project에 실행하지 않았고, 고유 이름의 disposable project만 생성·삭제했다.

동시 예약 경쟁은 여전히 Day 8과 같다. DB unique constraint는 두 예약 행의 최종
중복만 막으며 locking이나 의도한 동시 충돌 응답은 제공하지 않는다. 후속 task에서
별도로 다룬다.
