# Task: Day 6 Docker Compose 통합 실행 및 smoke test

## 목적

저장소 루트의 단일 `compose.yaml`로 기존 Dockerfile을 사용해 Spring Boot
`ticketing-api`, FastAPI `predictor`, PostgreSQL을 함께 빌드·실행한다. 세 서비스의
health 상태와 호스트에서 접근 가능한 애플리케이션 health endpoint, Spring JDBC의
실제 PostgreSQL 연결, PostgreSQL의 기본 쿼리 실행을 하나의 통합 smoke test로
검증해 이후 도메인 작업이 사용할 수 있는 재현 가능한 로컬 실행 기반을 만든다.

## 작업 범위

- 저장소 루트에 Compose Specification 형식의 `compose.yaml` 하나를 작성한다.
- `ticketing-api`와 `predictor`는 Day 5의 기존 Dockerfile과 각 애플리케이션
  디렉터리를 build context로 사용한다.
- PostgreSQL은 공식 이미지를 사용하고 로컬 개발용 데이터베이스 이름, 사용자,
  비밀이 아닌 개발 전용 비밀번호를 Compose 환경 변수로 명시한다.
- PostgreSQL 데이터는 `postgres-data` named volume에 저장해 일반적인
  `docker compose down` 이후에도 유지한다.
- `ticketing-api`에 Spring JDBC starter와 PostgreSQL 드라이버를 추가한다.
- Compose가 `ticketing-api`에 `postgres:5432`를 사용하는 datasource URL,
  사용자, 비밀번호를 환경 변수로 전달한다.
- `ticketing-api`는 PostgreSQL health check가 통과한 후 시작한다.
- Actuator DB health indicator가 실제 datasource 연결을 검사하고, PostgreSQL
  연결 실패 시 Spring 전체 health가 `UP`이 되지 않게 한다.
- Docker 없이 실행되는 Spring 테스트는 테스트 전용 H2를 사용해 기존 검증
  스크립트의 독립성을 유지한다.
- `ticketing-api`, `predictor`, PostgreSQL의 컨테이너 포트와 필요한 호스트
  게시 포트를 명시한다.
- 기존 애플리케이션 Dockerfile의 health check를 Compose에서도 단일 진실
  공급원으로 사용하고, PostgreSQL에는 `pg_isready` 기반 health check를 추가한다.
- 세 서비스가 동일한 Compose 프로젝트의 기본 네트워크에서 서비스 이름으로
  해석되는지 검증한다.
- 루트 Compose 스택을 빌드·기동하고 세 서비스가 제한 시간 안에 `healthy`가 되는지
  확인하는 통합 smoke test 스크립트를 작성한다.
- smoke test에서 호스트를 통해 `ticketing-api`의 `/actuator/health`와
  `predictor`의 `/health`를 호출하고, Spring 응답의 DB component를 포함한 기대
  상태를 검증한다.
- smoke test에서 PostgreSQL 컨테이너에 `SELECT 1`을 실행해 서버가 실제 쿼리를
  처리하는지 검증한다.
- smoke test에서 정상 상태 검증 후 PostgreSQL을 중지하고 Spring 전체 health와 DB
  component가 모두 `DOWN`이 되는지 검증한다.
- smoke test 성공·실패 여부와 관계없이 이번 검증에서 띄운 Compose 리소스를
  정리하도록 한다.
- 기존 `scripts/verify.sh`를 수정하지 않고 별도로 실행해 기존 Spring/Python
  검증 책임을 유지한다.
- 실제 실행 방법, 설계 판단, 검증 결과 및 남은 위험을 task 완료 결과에 기록한다.

## 제외 범위

- 티켓팅 비즈니스 API
- JPA Entity, Repository, ORM mapping 등 도메인 및 영속성 모델
- 비즈니스 테이블, schema migration 및 초기 데이터
- `ticketing-api`와 `predictor` 사이의 실제 API 호출
- ML 모델 학습 및 추론
- 부하 테스트 및 k6
- Kubernetes, kind 및 오토스케일링 구성
- GHCR을 포함한 이미지 레지스트리 게시
- GitHub Actions workflow 수정
- 로그인, 결제, 프론트엔드
- Redis, Kafka 및 분산 대기열
- 운영용 비밀 관리와 운영 배포 설정
- 기존 Dockerfile, `.dockerignore`, `scripts/verify.sh`의 변경
- Spring JDBC, PostgreSQL driver, 테스트 전용 H2 이외의 새로운 애플리케이션
  의존성 또는 컨테이너 오케스트레이션 도구 추가

## Acceptance Criteria

- [x] 저장소 루트에 유일한 Compose 정의 파일 `compose.yaml`이 있다.
- [x] `docker compose config --quiet`가 성공한다.
- [x] `ticketing-api` 서비스가 `ticketing-api/Dockerfile`과
  `ticketing-api` build context를 사용한다.
- [x] `predictor` 서비스가 `predictor/Dockerfile`과 `predictor` build context를
  사용한다.
- [x] PostgreSQL 서비스가 공식 PostgreSQL 이미지와 이름이 지정된 volume을
  사용한다.
- [x] 일반적인 `docker compose down` 이후에도 `postgres-data` volume이
  유지된다.
- [x] PostgreSQL health check가 `pg_isready`로 준비 상태를 검사한다.
- [x] `ticketing-api`가 Spring JDBC starter와 PostgreSQL 드라이버를 사용한다.
- [x] Compose가 `ticketing-api`에
  `jdbc:postgresql://postgres:5432/flashscale` datasource URL을 전달한다.
- [x] `ticketing-api`가 PostgreSQL이 `healthy`가 된 후 시작된다.
- [x] Actuator DB health indicator가 실제 PostgreSQL 연결을 검사한다.
- [x] PostgreSQL 연결 실패 시 Spring health가 `UP`으로 판정되지 않는다.
- [x] Spring 테스트가 H2를 사용해 Docker 없이 독립적으로 실행된다.
- [x] 애플리케이션 health check는 기존 Dockerfile 정의를 중복하지 않고 그대로
  사용한다.
- [x] 세 서비스가 하나의 명령으로 빌드·기동되고 제한 시간 안에 모두
  `healthy`가 된다.
- [x] 호스트에서 `ticketing-api`의 `/actuator/health` 호출이 HTTP 200,
  전체 `UP`, DB component `UP` 상태를 반환한다.
- [x] 호스트에서 `predictor`의 `/health` 호출이 HTTP 200과
  `{"status":"UP"}` 응답을 반환한다.
- [x] PostgreSQL에서 `SELECT 1`이 성공한다.
- [x] Compose 네트워크 안에서 `ticketing-api`, `predictor`, `postgres` 서비스
  이름을 해석할 수 있다.
- [x] `scripts/smoke-test.sh` 하나로 위 통합 검증을 재현할 수 있다.
- [x] smoke test 종료 후 생성한 컨테이너와 network가 정리된다.
- [x] 기존 Dockerfile, `.dockerignore`, `.github/workflows/ci.yml`,
  `scripts/verify.sh`를 변경하지 않았다.
- [x] 비즈니스 API, JPA 도메인 모델, 실제 서비스 간 API 연동, 부하 테스트,
  Kubernetes 및 GHCR 작업을 추가하지 않았다.
- [x] 저장소 루트의 `./scripts/verify.sh`가 통과한다.
- [x] 실제 변경 파일, 주요 설계 판단, 실행한 검증 명령과 결과, 남은 위험이
  문서화되었다.

## 검증 명령

```bash
docker compose config --quiet
./scripts/smoke-test.sh
./scripts/verify.sh
```

통합 smoke test는 프로세스 ID를 포함한 고유한 Compose project name을 사용해 다른
로컬 Compose 프로젝트와 충돌하지 않게 한다. 검증이 끝나면 성공·실패와 관계없이
해당 project의 컨테이너, network, volume을 제거한다.

## 예상 변경 파일

- `compose.yaml`
- `scripts/smoke-test.sh`
- `ticketing-api/build.gradle`
- `ticketing-api/src/main/resources/application.properties`
- `ticketing-api/src/test/resources/application.properties`
- `ticketing-api/src/test/java/com/eschoi04/ticketing_api/TicketingApiApplicationTests.java`
- `docs/tasks/day-06-compose-integration.md`
- `docs/retrospectives/day-06.md`

## 위험 요소

- Day 6은 `main`에 반영된 Day 5의 Dockerfile과 `.dockerignore`를 단일 진실
  공급원으로 사용한다.
- 최초 Compose 빌드는 기반 이미지, Gradle 배포본, Maven 의존성, pip 패키지,
  PostgreSQL 이미지를 내려받기 위한 네트워크 접근이 필요하다.
- 호스트의 기본 게시 포트 `8080`, `8000`, `5432`가 이미 사용 중이면 Compose
  기동이 실패할 수 있다. 필요 시 비즈니스 설정을 바꾸지 않는 범위에서 Day 6 전용
  호스트 포트를 사용한다.
- PostgreSQL의 이름이 지정된 volume은 일반 실행에서는 데이터를 유지하지만 smoke
  test는 재현성과 정리를 위해 자신이 만든 volume을 제거한다. 따라서 smoke test는
  프로세스 ID가 포함된 고유한 project name으로 다른 프로젝트와 구분한다.
- 이 task는 Spring JDBC 연결까지 검증하지만 JPA, migration, 비즈니스 스키마는
  만들지 않는다. 실제 도메인 테이블과 동시성 제어는 후속 task에서 다뤄야 한다.
- H2는 기존 Spring 검증을 Docker와 분리하기 위한 테스트 전용 대체재다. 통합 smoke
  test는 반드시 PostgreSQL을 사용해 데이터베이스 종류 차이로 인한 오판을 줄인다.
- Docker daemon을 사용할 수 없거나 현재 사용자에게 Docker 실행 권한이 없으면
  통합 검증이 제한될 수 있으며, 이 경우 원인과 수행하지 못한 검증을 기록한다.

## 완료 결과

- 저장소 루트의 `compose.yaml`에 `ticketing-api`, `predictor`, `postgres` 세
  서비스를 정의했다. 애플리케이션 이미지는 Day 5의 기존 Dockerfile을 그대로
  사용하고 PostgreSQL은 공식 `postgres:17-alpine` 이미지를 사용한다.
- 일반 개발 실행은 `docker compose up --build --detach`이며, 종료는
  `docker compose down`이다. PostgreSQL 데이터는 Compose가 관리하는
  `postgres-data` named volume에 남으므로 `--volumes`를 명시하지 않는 한
  컨테이너를 다시 만들어도 유지된다.
- `ticketing-api`에 `spring-boot-starter-jdbc`와 PostgreSQL runtime driver를
  추가했다. Compose는 datasource URL
  `jdbc:postgresql://postgres:5432/flashscale`과 로컬 개발용 계정을 환경변수로
  전달한다. `depends_on.condition: service_healthy`로 PostgreSQL 준비 후 Spring을
  시작한다.
- JPA, Entity, Repository, schema migration, DDL, 비즈니스 테이블은 추가하지
  않았다. 기존 검증이 Docker에 의존하지 않도록 H2는 테스트 runtime에서만
  사용한다.
- Actuator가 component 상태를 노출하게 해 DB health indicator 결과를 검증할 수
  있게 했다. Spring 테스트도 H2 DB component가 `UP`인지 확인한다.
- `scripts/smoke-test.sh`는 프로세스 ID가 포함된 전용 Compose project를 만들고
  세 이미지를 빌드·기동한 뒤 컨테이너 health, 두 HTTP health endpoint,
  PostgreSQL `SELECT 1`, Compose 서비스 이름 DNS 해석을 검증한다.
- 정상 상태에서 Spring Actuator 응답은 전체 `status=UP`과
  `components.db.status=UP`을 반환했다. 이후 PostgreSQL 컨테이너만 중지하자
  DB component와 전체 상태가 모두 `DOWN`으로 바뀌어 연결 실패가 정상 상태로
  판정되지 않음을 확인했다.
- smoke test의 전용 컨테이너, network, volume은 성공·실패 여부와 관계없이
  `trap`으로 제거한다. 고유 project name을 사용하므로 일반 개발용
  `postgres-data` volume에는 영향을 주지 않는다.
- `docker compose config --quiet`, `sh -n scripts/smoke-test.sh`,
  `git diff --check`, `./gradlew test`, `./scripts/smoke-test.sh`,
  `./scripts/verify.sh`가 모두 통과했다. Spring build와 Checkstyle, Spotless,
  테스트가 통과했고 Python format·lint와 pytest 1개도 통과했다.
- 기존 `ticketing-api/Dockerfile`, `predictor/Dockerfile`, 두 `.dockerignore`,
  `.github/workflows/ci.yml`, `scripts/verify.sh`는 변경하지 않았다.
- 남은 위험은 로컬 개발 비밀번호가 운영 비밀로 사용할 수 없는 공개 기본값이라는
  점, 이미지 태그와 Python 의존성이 digest 또는 lock으로 고정되지 않았다는 점,
  아직 migration과 비즈니스 스키마가 없어 데이터 구조를 검증하지 않는다는 점이다.
