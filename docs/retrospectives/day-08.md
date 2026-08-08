# Day 8 회고

## 오늘의 목표

이후 부하 테스트와 오토스케일링 실험에서 사용할 최소 티켓팅 도메인을 구현한다.
이벤트와 지정한 수의 좌석을 함께 생성하고, 이벤트·좌석을 조회하며, 예약 가능한
좌석을 예약할 수 있게 한다. 같은 좌석을 순차적으로 다시 예약하면 409로 거절한다.

## 구현한 도메인과 API

- `Event`는 티켓팅 대상의 이름과 여러 `Seat`를 가진다.
- `Seat`는 한 이벤트에 속하고 이벤트 안의 순번 및 `AVAILABLE`, `RESERVED` 상태를
  가진다.
- `Reservation`은 한 좌석과 고객 문자열 식별자를 연결한다.
- `POST /api/events`로 이벤트와 좌석을 함께 생성한다.
- `GET /api/events/{eventId}`로 이벤트를 조회한다.
- `GET /api/events/{eventId}/seats`로 좌석 목록과 상태를 조회한다.
- `POST /api/events/{eventId}/seats/{seatId}/reservations`로 좌석을 예약한다.
- 도메인 예외와 입력 검증 실패를 공통 JSON 오류 응답으로 반환한다.

## 주요 설계 결정

- 기존 패키지 규칙이 애플리케이션 진입점 외에는 없어서 기능 단위 `event` 패키지에
  Entity, Repository, DTO, Service, Controller를 모았다. 여러 기능에서 공유하는
  오류 응답과 전역 예외 처리는 `common.error`로 분리했다.
- `Event`와 `Seat`는 일대다·다대일 양방향 관계다. Event가 좌석 생성 생명주기를
  소유하고 cascade 저장하므로 이벤트 생성 Service는 Event 하나만 저장해도 같은
  트랜잭션 안에서 좌석들이 저장된다.
- `Seat`와 `Reservation`은 일대일 관계다. 외래 키는 예약 테이블의 `seat_id`에 두고
  unique 제약을 선언해 좌석당 예약 하나라는 구조를 DB에도 표현했다.
- 좌석 목록에 필요한 상태를 매번 예약 존재 여부로 계산하지 않고 Seat의 enum
  상태로 보관했다. `Seat.reserve()`가 상태 확인과 변경을 담당하므로 Service가
  도메인 상태 전이 규칙을 중복하지 않는다.
- `findByIdAndEvent_Id`는 좌석 ID와 이벤트 연관 객체의 ID를 동시에 조회한다. 따라서
  실제 좌석이 존재해도 URL의 이벤트와 소속이 다르면 좌석을 노출하지 않고 404로
  처리한다.
- 이벤트 생성과 예약은 원자적으로 끝나야 하므로 Service의 쓰기 메서드에
  `@Transactional`을 두었다. 조회 메서드는 `readOnly = true`로 의도를 표시했다.
- 별도 migration 의존성은 도입하지 않았다. 현재 초기 실험 단계의 최소 방식으로
  PostgreSQL은 Hibernate `update`, 테스트 H2는 `create-drop`을 사용한다. schema가
  안정되면 Flyway 같은 versioned migration 도구를 별도 task에서 검토해야 한다.

## 실제 검증 결과

- `./gradlew spotlessCheck checkstyleMain checkstyleTest test`: 통과
  - Day 8 HTTP 통합 테스트 6개 통과
  - 기존 애플리케이션 테스트 2개 통과
- `./scripts/verify.sh`: 통과
  - Spring 포맷, Checkstyle, 테스트 통과
  - Predictor Ruff 포맷·lint 통과
  - Predictor pytest 1개 통과
- `docker compose config --quiet`: `scripts/smoke-test.sh` 안에서 통과
- `./scripts/smoke-test.sh`: 통과
  - 세 컨테이너 `healthy`
  - PostgreSQL `SELECT 1` 성공
  - Spring DB health `UP`, PostgreSQL 중지 후 `DOWN`
- 실제 PostgreSQL API smoke test: 통과
  - 이벤트 ID 1과 좌석 3개 생성
  - 첫 좌석 상태 `AVAILABLE` 확인
  - 첫 예약 HTTP 201 및 상태 `RESERVED` 확인
  - 같은 좌석의 두 번째 순차 예약 HTTP 409와
    `SEAT_ALREADY_RESERVED` 확인
  - 테스트 전용 컨테이너, network, volume 제거

## 이번에 의도적으로 제외한 사항

- 비관적 잠금, 낙관적 잠금, 분산 락
- 실제 동시 요청의 중복 예약 방지
- 메시지 큐, 대기열, Redis, Kafka
- 로그인, 인증·인가, 결제, 예약 취소
- 좌석 등급과 가격 정책
- 부하 테스트, Kubernetes, Predictor 연동
- Flyway 또는 Liquibase 도입

## 발견한 문제 또는 기술 부채

- Spring Boot 4.1 테스트 환경은 Jackson 3의 `tools.jackson` 패키지를 사용한다.
  처음 Jackson 2 패키지를 참조해 컴파일이 실패했고 프로젝트 버전에 맞게 수정했다.
- Spring Data 파생 쿼리에서 DB 열 이름 `event_id`가 아니라 Java 연관 경로
  `event.id`를 표현해야 했다. `findByIdAndEvent_Id` 형태로 관계 탐색을 명시했다.
- Hibernate `ddl-auto=update`는 빠른 개발에는 작지만 schema 변경 이력, 명시적
  배포 순서와 rollback을 제공하지 않는다.
- H2 통합 테스트는 빠르고 독립적이지만 PostgreSQL과 완전히 같지 않다. 이번에는
  실제 Compose/PostgreSQL API smoke test로 주요 차이를 보완했다.
- 이벤트 생성은 좌석 수만큼 객체와 insert를 한 트랜잭션에서 만든다. 매우 큰
  좌석 수의 batching 및 성능은 아직 측정하지 않았다.

## 다음 단계에서 다룰 동시성 문제

현재 예약 흐름은 트랜잭션 안에서 좌석을 읽고 `AVAILABLE`인지 확인한 뒤 상태와 예약
행을 저장한다. 두 요청이 동시에 같은 좌석의 `AVAILABLE` 상태를 읽으면 둘 다 상태
검사를 통과할 수 있다. 예약 테이블의 unique 제약이 두 예약 행의 최종 저장은 막을
수 있지만, 현재는 그 DB 충돌을 의도한 409 응답으로 변환하지 않으며 좌석 상태 갱신의
경쟁도 명시적으로 제어하지 않는다.

다음 단계에서는 먼저 실제 동시 요청 테스트로 이 경쟁 조건을 재현하고, 비관적 잠금과
낙관적 잠금 같은 후보를 같은 조건에서 비교해야 한다. 선택한 방식은 중복 예약 방지뿐
아니라 티켓팅 부하에서의 처리량, 지연시간, DB lock 대기와 재시도 비용까지 측정해야
한다.

## 내일 첫 번째 작업

동일 좌석에 대한 동시 예약 테스트의 요청 수, 성공 기대값과 관측할 DB·HTTP 결과를
먼저 task 문서로 정의한다. 구현 전략을 고르기 전에 현재 Day 8 코드에서 경쟁 조건을
재현해 기준 결과를 남긴다.
