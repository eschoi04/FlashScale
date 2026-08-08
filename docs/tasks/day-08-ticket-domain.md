# Task: Day 8 최소 티켓팅 도메인과 API

## 목적

이후 부하 테스트와 오토스케일링 실험에서 현실적인 HTTP 요청과 데이터베이스 부하를
만들 수 있도록 `ticketing-api`에 이벤트, 좌석, 예약의 최소 도메인과 REST API를
구현한다. 단일 요청 환경에서 이벤트와 좌석을 생성·조회하고 좌석을 한 번 예약할 수
있으며, 같은 좌석을 순차적으로 다시 예약하면 일관된 오류 응답과 함께 거절되게 한다.

## 작업 범위

- Spring Data JPA 의존성을 추가하고 기존 PostgreSQL datasource 및 테스트용 H2와
  함께 동작하게 한다.
- `Event`, `Seat`, `Reservation` JPA Entity와 이벤트-좌석, 좌석-예약 관계를
  정의한다.
- 좌석의 예약 가능·예약 완료 상태를 enum으로 표현한다.
- 기능 단위 패키지 안에 Repository, 요청·응답 DTO, Service, Controller를 둔다.
- `POST /api/events`에서 이벤트 이름과 좌석 수를 검증하고 이벤트와 좌석을 하나의
  트랜잭션으로 생성한다.
- `GET /api/events/{eventId}`에서 이벤트를 조회한다.
- `GET /api/events/{eventId}/seats`에서 해당 이벤트의 좌석 목록을 조회한다.
- `POST /api/events/{eventId}/seats/{seatId}/reservations`에서 고객 식별자를
  검증하고 해당 이벤트 소속의 예약 가능한 좌석을 하나의 트랜잭션으로 예약한다.
- 존재하지 않는 이벤트·좌석은 404, 이미 예약된 좌석은 409, 입력 검증 실패는
  400으로 처리한다.
- 도메인 예외와 검증 예외를 일관된 JSON 오류 응답으로 변환하는 전역 예외 처리를
  구현한다.
- 별도 migration 도구를 새로 도입하지 않고 현재 초기 개발 단계에 맞춰 Hibernate
  schema 초기화 설정을 사용한다. 운영 PostgreSQL과 테스트 H2의 설정을 명시적으로
  구분한다.
- MockMvc 기반 통합 테스트로 생성, 조회, 예약, 상태 변경, 순차 중복 예약과 주요
  오류 계약을 검증한다.
- 가능하면 Compose 환경에서 실제 PostgreSQL을 사용해 핵심 API 흐름을 smoke
  test하고 기존 서비스 health 검증이 깨지지 않았는지 확인한다.
- 실제 구현과 검증 결과를 Day 8 task 완료 결과와 회고에 기록한다.

## 제외 범위

- 비관적 잠금, 낙관적 잠금, 분산 락 및 동시 예약의 완전한 해결
- 메시지 큐, 대기열, Redis, Kafka 및 cache
- 로그인, 인증·인가, 결제 및 사용자 관리
- 좌석 등급, 가격 정책 및 예약 취소
- 부하 테스트와 k6
- Kubernetes, kind 및 오토스케일링 구성
- Predictor 연동과 ML 모델 변경
- 프론트엔드
- 현재 최소 기능에 필요하지 않은 공통 추상화와 무관한 리팩터링
- Flyway 또는 Liquibase 등 새로운 schema migration 도구 도입

## Acceptance Criteria

- [x] Day 8 작업 문서가 템플릿에 맞게 작성되고 구현 전에 사용자 확인을 받았다.
- [x] 이벤트 이름과 좌석 수로 이벤트를 생성하면 요청한 수만큼 좌석이 함께 생성된다.
- [x] 생성한 이벤트를 단건 조회할 수 있다.
- [x] 이벤트에 속한 좌석 목록을 조회할 수 있다.
- [x] 예약 가능한 좌석을 고객 식별자로 예약할 수 있다.
- [x] 예약 후 해당 좌석의 상태가 예약 완료로 조회된다.
- [x] 같은 좌석을 순차적으로 다시 예약하면 `409 Conflict`가 반환된다.
- [x] 존재하지 않는 이벤트 조회는 `404 Not Found`를 반환한다.
- [x] 존재하지 않거나 해당 이벤트에 속하지 않는 좌석 예약은 `404 Not Found`를
  반환한다.
- [x] 빈 이벤트 이름, 0 이하의 좌석 수 및 빈 고객 식별자는 `400 Bad Request`를
  반환한다.
- [x] API가 JPA Entity를 직접 노출하지 않고 요청·응답 DTO를 사용한다.
- [x] Controller는 요청 검증과 응답 변환만, Service는 비즈니스 로직과 트랜잭션
  경계를 담당한다.
- [x] 오류 응답이 일관된 JSON 구조를 사용한다.
- [x] JPA mapping과 schema 초기화가 PostgreSQL 및 테스트용 H2에서 동작한다.
- [x] 요구된 9개 시나리오가 자동화된 테스트로 검증된다.
- [x] `ticketing-api`의 `./gradlew test`가 통과한다.
- [x] 저장소 루트의 `./scripts/verify.sh`가 통과한다.
- [x] 가능한 경우 Compose/PostgreSQL 환경에서 핵심 API smoke test가 통과한다.
- [x] Day 8 회고가 실제 구현, 검증 결과, 제외 사항과 동시성 한계를 기록한다.
- [x] Day 8 범위 밖 기능과 요청과 무관한 변경이 포함되지 않는다.

## 검증 명령

```bash
cd ticketing-api
./gradlew test

cd ..
./scripts/verify.sh
docker compose config --quiet
./scripts/smoke-test.sh
git diff --check
```

도메인 API 통합 테스트는 H2를 사용하는 Spring test context에서 실행한다. Docker
daemon을 사용할 수 있으면 Compose로 실제 PostgreSQL 스키마 생성과 핵심 API 흐름도
별도로 확인하며, 실행하지 못하면 이유와 미검증 범위를 기록한다.

## 예상 변경 파일

- `docs/tasks/day-08-ticket-domain.md`
- `docs/retrospectives/day-08.md`
- `ticketing-api/build.gradle`
- `ticketing-api/src/main/resources/application.properties`
- `ticketing-api/src/test/resources/application.properties`
- `ticketing-api/src/main/java/com/eschoi04/ticketing_api/event/**`
- `ticketing-api/src/main/java/com/eschoi04/ticketing_api/common/error/**`
- `ticketing-api/src/test/java/com/eschoi04/ticketing_api/event/**`

구현 중 실제 책임에 따라 위 기능 패키지 내부 파일명은 구체화한다. 기존 Compose,
Dockerfile, Predictor, CI/CD 파일은 변경할 필요가 없을 것으로 예상한다.

## 위험 요소

- 일반적인 `@Transactional`과 애플리케이션 수준 상태 확인만으로는 같은 좌석에 대한
  실제 동시 요청의 경쟁 조건을 막지 못한다. Day 8은 순차 중복 예약만 보장하고,
  동시성 재현과 잠금 전략은 후속 task에서 다룬다.
- Hibernate 자동 schema 생성은 초기 개발과 실험 환경에는 작지만, 운영 단계의
  versioned migration과 rollback 이력을 제공하지 않는다. 도메인 스키마가 안정된 뒤
  migration 도구 도입을 별도 결정해야 한다.
- H2와 PostgreSQL은 SQL dialect와 제약 동작이 완전히 같지 않다. 빠른 자동 테스트는
  H2로 유지하되 가능한 Compose smoke test로 PostgreSQL 경로를 보완한다.
- 이벤트 생성 시 좌석 수만큼 Entity를 한 트랜잭션에서 생성하므로 매우 큰 좌석 수는
  메모리와 insert 부하를 증가시킨다. 이번 task에서는 최소 입력 검증만 구현하고
  임의의 batching·비동기 생성 기능을 추가하지 않는다.
- Day 8 브랜치는 아직 `origin/main`에 없는 Day 7 완료 커밋 위에서 시작했다. 향후 PR
  전에는 Day 7의 `main` 병합 여부를 확인하고 `origin/main...HEAD` 범위를 재검증해야
  한다.

## 완료 결과

- Spring JDBC starter를 Spring Data JPA starter로 교체했다. JPA starter가 JDBC와
  transaction 기능을 함께 제공하므로 기존 datasource와 Actuator DB health 동작은
  유지된다. PostgreSQL runtime driver와 테스트용 H2도 그대로 사용한다.
- 운영·Compose 환경에는 `spring.jpa.hibernate.ddl-auto=update`, 테스트 환경에는
  `create-drop`을 설정했다. 현재 초기 실험 단계에서 추가 의존성이 없는 가장 작은
  schema 초기화 방식이며, versioned migration 부재는 후속 기술 부채로 남겼다.
- `Event`가 좌석을 생성하고 `cascade = ALL`로 함께 저장한다. `Seat`는 이벤트를
  지연 로딩으로 참조하고 `(event_id, seat_number)` 유일 제약을 가진다.
  `Reservation`은 좌석을 지연 로딩으로 참조하며 `seat_id` 유일 제약으로 좌석당
  예약 행 하나라는 데이터 모델을 표현한다.
- 좌석은 `AVAILABLE`과 `RESERVED` 상태를 갖는다. 예약 Service가 좌석을 조회한 뒤
  `Seat.reserve()`로 상태를 변경하고 Reservation을 저장한다. 이미 `RESERVED`이면
  저장 전에 `SeatAlreadyReservedException`을 던져 전역 예외 처리기가 409로
  변환한다.
- 이벤트 생성과 좌석 예약은 각각 `@Transactional` 쓰기 트랜잭션 하나로 묶었다.
  이벤트·좌석 조회는 `@Transactional(readOnly = true)`를 사용한다. Controller에는
  입력 검증과 HTTP 상태 선언만 두고 Entity를 API에 직접 노출하지 않는다.
- `EventNotFoundException`, `SeatNotFoundException`,
  `SeatAlreadyReservedException`과 Bean Validation 실패를 `ApiErrorResponse`의
  timestamp, status, error, code, message, path, fieldErrors 구조로 반환한다.
- `EventApiIntegrationTests`의 6개 HTTP 통합 테스트 안에서 요구된 9개 시나리오를
  검증했다. 기존 2개 애플리케이션 테스트를 포함해 Spring 테스트 8개가 통과했다.
- `./gradlew spotlessCheck checkstyleMain checkstyleTest test`와
  `./scripts/verify.sh`가 통과했다. Predictor Ruff·pytest도 통과했으며 Python
  테스트는 1개가 성공했다.
- `./scripts/smoke-test.sh`가 실제 PostgreSQL 연결, 세 컨테이너 health, `SELECT 1`,
  서비스 이름 해석과 PostgreSQL 장애 시 Spring health `DOWN`을 확인했다.
- 별도 Compose API smoke test에서 이벤트와 좌석 3개를 생성하고 첫 좌석을 예약했다.
  좌석 상태가 `AVAILABLE`에서 `RESERVED`로 바뀌었고 두 번째 순차 예약은 HTTP 409와
  `SEAT_ALREADY_RESERVED`를 반환했다. 검증용 컨테이너, network, volume은 제거했다.
- 비관적·낙관적 잠금 등 동시성 제어, 인증, 결제, 취소, 부하 테스트, Predictor
  연동, Redis, 메시지 큐, Kubernetes와 migration 도구는 추가하지 않았다.
