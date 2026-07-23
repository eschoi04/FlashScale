# Day 2 회고

## 오늘의 목표

- `ticketing-api`를 독립 실행 가능한 Spring Boot 애플리케이션으로 초기화한다.
- `predictor`를 독립 실행 가능한 FastAPI 애플리케이션으로 초기화한다.
- 두 애플리케이션에 health endpoint와 기본 테스트를 추가한다.
- 로컬 실행 및 테스트 방법을 문서화하고 프로젝트별 생성물 제외 규칙을 정리한다.
- 티켓팅 비즈니스 로직과 인프라 등 Day 2 범위 밖 기능은 추가하지 않는다.

## 완료한 작업

- Spring Boot 애플리케이션과 Gradle wrapper를 구성했다.
- Spring Actuator의 `/actuator/health` endpoint와 context load 테스트를 검증했다.
- FastAPI Predictor와 Uvicorn 실행 환경을 구성했다.
- Predictor의 `/health` endpoint와 pytest 기반 기본 테스트를 추가했다.
- 런타임 의존성과 테스트 의존성을 각각 `requirements.txt`와 `requirements-test.txt`로 분리했다.
- 루트 `.gitignore`는 저장소 공통 규칙을, 각 애플리케이션의 `.gitignore`는 프로젝트 전용 규칙을 담당하도록 정리했다.
- README에 두 애플리케이션의 환경 준비, 실행, health 확인, 테스트 방법을 문서화했다.
- Day 2 체크리스트 15개 항목을 모두 검증하고 완료로 표시했다.
- 최신 `main`에서 `agent/day-02-app-bootstrap` 브랜치를 만들고 기존 미커밋 작업을 보존했다.

## 이해한 개념

- Spring Boot의 `./gradlew bootRun`에 대응하는 FastAPI 실행 방식은 Uvicorn으로 ASGI 앱을 구동하는 `.venv/bin/python -m uvicorn app.main:app --reload`이다.
- `app.main:app`에서 앞부분은 Python 모듈 경로이고 마지막 `app`은 해당 모듈에 선언된 FastAPI 객체 이름이다.
- 테스트 통과와 실제 프로세스 실행은 서로 다른 검증이다. `TestClient`와 context test는 애플리케이션 내부 계약을 빠르게 확인하고, 실제 서버 기동 후 health 요청은 포트 바인딩과 런타임 구성을 함께 확인한다.
- 모노레포의 `.gitignore`는 루트에서 공통 규칙을 관리하고 하위 애플리케이션에서 언어·빌드 도구별 규칙을 관리하면 책임과 적용 범위가 명확해진다.
- Git 브랜치는 커밋을 가리키는 포인터이고 미커밋 파일은 작업 디렉터리의 상태다. 대상 브랜치와 경로 충돌이 없다면 stash 없이도 변경을 유지한 채 새 브랜치로 이동할 수 있다.
- `git branch -f main origin/main`은 현재 작업 디렉터리를 바꾸지 않고 로컬 `main` 포인터만 안전하게 fast-forward할 때 사용할 수 있다. 사용 전에는 `git merge-base --is-ancestor`로 로컬 전용 커밋이 없는지 확인해야 한다.

## 막힌 부분과 해결 과정

- 미커밋된 `AGENTS.md` 때문에 `git switch main`이 파일 덮어쓰기 위험을 감지하고 중단했다. 로컬 `main`이 `origin/main`의 조상임을 확인한 뒤 작업 디렉터리를 건드리지 않고 `main` 포인터를 최신 커밋으로 이동하고 Day 2 브랜치를 생성했다.
- 샌드박스에서 PyPI 네트워크 접근이 차단되어 FastAPI 의존성 설치가 실패했다. 승인된 외부 네트워크 접근으로 Predictor 가상환경에만 의존성을 설치했다.
- FastAPI 공식 테스트 예제에 따라 `httpx`를 설치했지만 최신 Starlette에서 deprecation 경고가 발생했다. 설치된 Starlette가 `httpx2`를 우선 사용하도록 구현된 것을 확인하고 테스트 의존성을 교체해 경고 없이 테스트를 통과시켰다.
- 샌드박스에서는 로컬 포트 바인딩이 제한되어 서버 실행이 실패했다. 승인된 로컬 실행 권한으로 Spring Boot와 Uvicorn을 기동하고 각각의 health endpoint를 확인한 뒤 정상 종료했다.

## Codex가 제안했지만 채택하지 않은 내용

- Python 의존성 버전 고정은 재현성에 도움이 되지만, Day 2의 최소 부트스트랩 범위를 넘어 별도 정책 결정이 필요하므로 적용하지 않았다.
- `scripts/verify.sh` 구현은 공통 검증을 단순화할 수 있지만 Day 3 작업으로 명시되어 있어 추가하지 않았다.
- 루트 `.gitignore`에 Java와 Python 규칙을 모두 두는 방식은 동작하지만 프로젝트별 책임이 중복되므로 채택하지 않았다.

## 검증 결과

- `ticketing-api`에서 `./gradlew test`가 통과했다.
- Spring Boot를 실제 실행하고 `GET /actuator/health`가 `UP`을 반환하는 것을 확인했다.
- `predictor`에서 `.venv/bin/python -m pytest`가 경고 없이 통과했다.
- Uvicorn을 실제 실행하고 `GET /health`가 HTTP 200과 `{"status":"UP"}`을 반환하는 것을 확인했다.
- Gradle 캐시·빌드 결과와 Python 가상환경·캐시가 각 프로젝트의 `.gitignore`에 의해 제외되는 것을 확인했다.
- Docker, Docker Compose, Kubernetes, PostgreSQL 연결, 앱 간 통신, ML 학습·추론 코드가 추가되지 않은 것을 확인했다.
- Day 2 변경 범위의 `git diff --check`가 통과했다.
- `./scripts/verify.sh`는 Day 3 범위로 아직 존재하지 않아 실행하지 못했다.

## 남은 위험

- Python 의존성 버전이 고정되어 있지 않아 설치 시점에 따라 간접 의존성 버전이 달라질 수 있다.
- 현재 테스트는 애플리케이션 부트스트랩과 health endpoint만 다루며 비즈니스 기능은 검증하지 않는다.
- 공통 검증 진입점인 `scripts/verify.sh`가 없어 Spring과 Predictor 테스트를 각각 실행해야 한다.
- 로컬 개발 명령은 macOS/Linux 계열 경로를 기준으로 하며 Windows 가상환경 실행 방법은 아직 문서화하지 않았다.

## 내일 첫 번째 작업

- `AGENTS.md`, 프로젝트 차터와 Day 3 활성 task를 읽고 범위를 확인한 뒤, 두 애플리케이션의 검증을 한 번에 실행하는 `scripts/verify.sh`의 acceptance criteria를 정리한다.
