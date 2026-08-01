# Day 6 회고

## 오늘의 목표

루트 Compose 파일 하나로 Spring Boot `ticketing-api`, FastAPI `predictor`,
PostgreSQL을 실행하고, Spring의 실제 PostgreSQL 연결을 포함한 통합 smoke test로
검증한다.

## 완료한 작업

- Day 5 컨테이너화 변경을 먼저 `main`에 반영하고 원격 저장소에 push한 뒤 갱신된
  `main`에서 Day 6 브랜치를 다시 만들었다.
- 루트 `compose.yaml`에 세 서비스와 `postgres-data` named volume을 정의했다.
- Spring JDBC starter와 PostgreSQL runtime driver를 추가했다.
- Compose 환경변수로 Spring datasource가 `postgres:5432`를 사용하게 했다.
- PostgreSQL health check와 Spring의 시작 순서를 연결했다.
- Spring 테스트에는 H2 datasource를 사용하고 DB health assertion을 추가했다.
- 정상 및 PostgreSQL 장애 상태를 모두 검사하는 `scripts/smoke-test.sh`를 만들었다.

## 이해한 개념

- Compose 서비스 이름은 내부 DNS 이름이므로 Spring 컨테이너는 호스트 공개 포트가
  아니라 `postgres:5432`로 PostgreSQL에 접속한다.
- named volume은 컨테이너 생명주기와 데이터 생명주기를 분리한다.
  `docker compose down`은 데이터를 보존하지만 `down --volumes`는 삭제한다.
- Spring JDBC starter가 만든 datasource는 Actuator DB health indicator에
  자동으로 참여한다. DB 연결이 실패하면 DB component와 전체 health가 `DOWN`이
  된다.
- 단위·애플리케이션 테스트의 H2와 통합 smoke test의 PostgreSQL은 책임이 다르다.
  전자는 빠르고 독립적인 검증을, 후자는 실제 운영 DB 종류와 네트워크 경로 검증을
  담당한다.

## 막힌 부분과 해결 과정

- 처음에는 “JPA 도메인 모델 제외”를 datasource 연결 제외까지 넓게 해석했다.
  요구사항을 다시 확인해 JPA·비즈니스 스키마와 JDBC 연결을 분리하고, JDBC 실제
  연결은 Day 6의 핵심 범위로 task 문서를 바로잡았다.
- 최초 smoke test는 Docker daemon이 실행되지 않아 실패했다. Docker Desktop을
  시작한 뒤 동일한 스크립트를 재실행해 검증했다.
- Spring 이미지가 새 의존성을 포함하면서 Gradle 배포본과 의존성을 다시 받아 첫
  빌드 시간이 길었지만, 이후 빌드는 Docker cache를 사용했다.

## Codex가 제안했지만 채택하지 않은 내용

- Spring과 PostgreSQL 연결을 후속 task로 미루는 초기 해석은 채택하지 않았다.
  Day 6에서 실제 datasource 연결과 장애 상태까지 검증하는 것으로 수정했다.
- 개인 또는 운영 DB 비밀번호를 저장소에 추가하지 않았다. Compose에는 로컬 개발
  전용 공개 기본값만 두었다.

## 검증 결과

- `docker compose config --quiet`: 통과
- `sh -n scripts/smoke-test.sh`: 통과
- `git diff --check`: 통과
- `./gradlew test`: 통과, H2 DB health `UP` 확인
- `./scripts/smoke-test.sh`: 통과
  - PostgreSQL, Spring, Predictor 컨테이너 모두 `healthy`
  - PostgreSQL `SELECT 1` 성공
  - Spring 전체 health 및 DB component `UP`
  - Predictor health `UP`
  - Compose 내부 서비스 이름 해석 성공
  - PostgreSQL 중지 후 Spring 전체 health 및 DB component `DOWN`
  - 테스트 전용 컨테이너, network, volume 제거
- `./scripts/verify.sh`: Spring 및 Python 전체 검증 통과, pytest 1개 통과

## 남은 위험

- 로컬 개발 계정과 비밀번호는 운영 환경에서 사용할 수 없다. 운영 단계에서는
  별도의 비밀 주입 방식이 필요하다.
- 아직 migration, JPA 모델, 비즈니스 테이블이 없어 실제 데이터 구조와 동시성
  동작은 검증하지 않는다.
- PostgreSQL 및 기반 이미지 태그와 Python 의존성이 digest 또는 lock으로 고정되지
  않아 향후 빌드 결과가 달라질 수 있다.
- smoke test는 로컬 Docker Desktop 환경에서 검증했으며 CI 통합은 이번 범위가
  아니다.

## 내일 첫 번째 작업

다음 활성 task를 프로젝트 차터와 템플릿에 맞춰 정의하고, PostgreSQL schema와
티켓팅 도메인 모델을 도입할 경우 동시성 안전성 기준을 acceptance criteria에 먼저
명시한다.
