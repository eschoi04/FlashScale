# Task: Day 2 애플리케이션 부트스트랩

## 목적

FlashScale의 두 애플리케이션인 Spring Boot `ticketing-api`와 Python `predictor`를 서로 독립적으로 실행하고 기본 상태를 확인할 수 있는 최소 기반으로 구성한다.

## 작업 범위

- `ticketing-api`를 Gradle wrapper 기반 Spring Boot 애플리케이션으로 초기화한다.
- Spring Actuator health endpoint와 기본 context load 테스트를 구성한다.
- `predictor`를 FastAPI 및 Uvicorn 기반 애플리케이션으로 초기화한다.
- Predictor health endpoint와 자동 테스트를 구성한다.
- Python 런타임 의존성과 테스트 의존성을 분리한다.
- 두 애플리케이션의 환경 준비, 실행, health 확인 및 테스트 방법을 문서화한다.
- 루트와 각 애플리케이션의 `.gitignore` 책임을 분리한다.

## 제외 범위

- 티켓팅 비즈니스 로직
- PostgreSQL 및 JPA
- Spring과 Predictor 간 통신
- ML 모델 학습 및 추론
- Docker와 Docker Compose
- Kubernetes 구성
- Day 3 공통 검증 스크립트

## Acceptance Criteria

- [x] `ticketing-api`에 독립적으로 실행 가능한 Spring Boot 프로젝트가 초기화되었다.
- [x] Spring Actuator의 health endpoint가 정상 응답한다.
- [x] Spring 애플리케이션의 기본 테스트가 통과한다.
- [x] `predictor`에 독립적으로 실행 가능한 FastAPI 프로젝트가 초기화되었다.
- [x] Predictor의 health endpoint가 정상 응답한다.
- [x] FastAPI 애플리케이션의 기본 테스트가 통과한다.
- [x] 두 애플리케이션의 로컬 실행 및 테스트 방법이 문서화되었다.
- [x] 루트 `.gitignore`는 저장소 공통 산출물을, `ticketing-api`와 `predictor`의 `.gitignore`는 각 애플리케이션 전용 산출물을 제외하도록 책임이 분리되었다.
- [x] 티켓팅 비즈니스 로직을 구현하지 않았다.
- [x] PostgreSQL 연결을 추가하지 않았다.
- [x] Spring과 Predictor 간 통신을 구현하지 않았다.
- [x] ML 모델 학습 및 추론을 구현하지 않았다.
- [x] Docker와 Docker Compose 구성을 추가하지 않았다.
- [x] Kubernetes 구성을 추가하지 않았다.
- [x] Day 3 범위인 `scripts/verify.sh`를 구현하지 않았다.

## 검증 명령

```bash
cd ticketing-api
./gradlew test
./gradlew bootRun
curl --fail http://localhost:8080/actuator/health

cd ../predictor
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements-test.txt
.venv/bin/python -m pytest
.venv/bin/python -m uvicorn app.main:app --reload
curl --fail http://localhost:8000/health
```

서버 실행과 `curl` 명령은 각각 별도 터미널에서 수행하고, 확인 후 서버를 종료한다. Day 2에는 `scripts/verify.sh`가 아직 없으므로 각 애플리케이션을 개별 검증한다.

## 예상 변경 파일

- `.gitignore`
- `AGENTS.md`
- `README.md`
- `docs/tasks/day-02-app-bootstrap.md`
- `docs/retrospectives/day-02.md`
- `ticketing-api/build.gradle`
- `ticketing-api/settings.gradle`
- `ticketing-api/gradlew`
- `ticketing-api/gradlew.bat`
- `ticketing-api/gradle/wrapper/*`
- `ticketing-api/src/main/java/com/eschoi04/ticketing_api/TicketingApiApplication.java`
- `ticketing-api/src/main/resources/application.properties`
- `ticketing-api/src/test/java/com/eschoi04/ticketing_api/TicketingApiApplicationTests.java`
- `ticketing-api/.gitignore`
- `ticketing-api/.gitattributes`
- `predictor/app/__init__.py`
- `predictor/app/main.py`
- `predictor/tests/test_health.py`
- `predictor/requirements.txt`
- `predictor/requirements-test.txt`
- `predictor/.gitignore`

## 위험 요소

- 최초 Gradle 및 Python 의존성 준비에는 네트워크 접근이 필요할 수 있다.
- 테스트 통과만으로 실제 프로세스의 포트 바인딩과 런타임 health 응답을 모두 보장하지 않으므로 두 애플리케이션을 직접 실행해 확인한다.
- Python 의존성 버전이 고정되어 있지 않아 설치 시점에 따라 간접 의존성 버전이 달라질 수 있다.
- 공통 검증 진입점은 Day 3 범위이므로 Day 2에는 Spring과 Predictor를 각각 실행해야 한다.

## 완료 결과

- Spring Boot 애플리케이션과 Gradle wrapper를 구성하고 `/actuator/health`의 `UP` 응답 및 context load 테스트를 확인했다.
- FastAPI Predictor와 `/health` 자동 테스트를 구성하고 HTTP 200 및 `{"status":"UP"}` 응답을 확인했다.
- 런타임·테스트 의존성과 애플리케이션별 `.gitignore` 책임을 분리했다.
- README에 환경 준비, 실행, health 확인 및 테스트 방법을 기록했다.
- 범위 밖 비즈니스 로직, 데이터베이스, 앱 간 통신 및 인프라 구성을 추가하지 않았다.
