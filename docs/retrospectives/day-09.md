# Day 9 회고

## 오늘 구현한 내용

- Spring Boot 4.1용 Flyway starter와 PostgreSQL 모듈을 추가했다.
- `Event`, `Seat`, `Reservation`을 PostgreSQL DDL로 명시한 V1 migration을 만들었다.
- 운영·로컬 PostgreSQL의 Hibernate 설정을 `ddl-auto=validate`로 전환했다.
- H2 테스트 격리는 유지하고, Testcontainers PostgreSQL migration 통합 테스트와
  Compose의 Flyway·API·재시작 smoke 검증을 추가했다.

## Flyway와 `ddl-auto`의 역할 차이

Flyway는 버전이 붙은 SQL을 순서대로 한 번씩 적용하고 schema 변경 이력을 남긴다.
Hibernate `ddl-auto=update`처럼 실행 시점에 Entity를 보고 DB를 임의 변경하지 않는다.
Hibernate의 `validate`는 schema를 만들지 않고 Entity mapping과 실제 schema가 맞지
않으면 애플리케이션 시작을 실패시킨다.

## 검증 결과

- `ticketing-api/./gradlew test`: 통과, H2 기존 테스트 8개와 PostgreSQL migration
  테스트 1개
- `./scripts/verify.sh`: 통과, Spring 포맷·Checkstyle·테스트와 Predictor Ruff·pytest
- `./scripts/smoke-test.sh`: 통과, 빈 PostgreSQL V1 적용, history 생성, 세 서비스
  health, 티켓팅 예약 API, 재시작 후 V1 1건 유지, DB 장애 health 확인
- 자동 검증은 고유 Compose project와 disposable volume만 사용했고 기존 volume은
  삭제하지 않았다.

## 막혔던 점 또는 남은 위험

- Spring Boot 4에서는 Flyway 자동설정이 별도 starter로 분리되어 `flyway-core`만으로는
  migration이 실행되지 않았다. `spring-boot-starter-flyway`로 교정했다.
- 기존 `ddl-auto=update` DB에는 Flyway history가 없어 그대로 시작할 수 없다. 초기
  개발 데이터를 버릴 수 있으면 백업 후 Compose volume을 일회성으로 다시 만들고,
  데이터를 보존해야 하면 별도의 이관 migration이 필요하다.
- 예약 unique constraint는 동시 예약의 애플리케이션 동작 전체를 해결하지 않는다.

## 다음 작업에서 이어갈 내용

동일 좌석 동시 예약을 재현하는 테스트를 먼저 만들고, 잠금 전략별 정확성·지연시간·DB
대기 비용을 비교한다. 기존 데이터 보존이 필요해지면 초기화 대신 명시적 이관
migration을 별도 task로 설계한다.
