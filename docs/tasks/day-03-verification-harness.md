# Task: Day 3 통합 검증 하네스

## 목적

Spring Boot 애플리케이션과 Python 예측 컴포넌트의 코드 품질 및 테스트를 하나의 루트 명령으로 반복 검증할 수 있게 한다. 로컬에서 어느 작업 디렉터리에서 실행하더라도 같은 검증 결과를 내고, 한 단계라도 실패하면 호출자에게 실패 상태를 정확히 전달한다.

## 작업 범위

- Spring 코드의 포맷 검사를 Gradle 검증 과정에 추가한다.
- Spring 코드의 lint/static analysis를 Gradle 검증 과정에 추가한다.
- Python 코드의 포맷 검사와 lint를 테스트 의존성 및 설정에 추가한다.
- Spring 테스트와 Python 테스트를 포함하는 루트 `scripts/verify.sh`를 구현한다.
- 로컬 검증 도구의 설치 및 전체 검증 실행 방법을 문서화한다.
- 검증 성공, 실패 전파, 작업 디렉터리 독립성을 확인한다.

## 제외 범위

- DB 및 PostgreSQL 연결
- Docker와 Docker Compose
- CI/GitHub Actions
- Kubernetes 및 배포 구성
- 티켓팅 비즈니스 로직과 예측 모델
- Day 3 검증에 필요하지 않은 리팩터링

## Acceptance Criteria

- [x] Spring formatter 검사가 구성되고 통과한다.
- [x] Spring lint/static analysis가 구성되고 통과한다.
- [x] Spring Actuator health endpoint의 상태 코드와 응답 상태를 자동 테스트한다.
- [x] Spring 테스트가 통과한다.
- [x] Python formatter 검사가 구성되고 통과한다.
- [x] Python lint가 구성되고 통과한다.
- [x] Predictor health endpoint의 상태 코드와 응답 내용을 자동 테스트한다.
- [x] Python 테스트가 통과한다.
- [x] 루트의 `./scripts/verify.sh` 하나로 모든 검사를 실행할 수 있다.
- [x] 어느 검사든 실패하면 `./scripts/verify.sh`의 exit code가 0이 아니다.
- [x] 스크립트가 현재 작업 디렉터리와 무관하게 저장소 경로를 올바르게 계산한다.
- [x] 추가한 검증 도구의 선택 이유와 고려한 대안이 문서화되었다.
- [x] 검증 환경 준비 방법과 개별/전체 검증 명령이 실제 동작과 일치한다.
- [x] DB, Docker, CI 등 Day 3 외 기능을 추가하지 않았다.
- [x] `AGENTS.md`와 `docs/project-charter.md`의 범위를 준수했다.

## 검증 명령

```bash
cd ticketing-api
./gradlew spotlessCheck checkstyleMain checkstyleTest test

cd ../predictor
.venv/bin/python -m ruff format --check .
.venv/bin/python -m ruff check .
.venv/bin/python -m pytest

cd ..
./scripts/verify.sh

cd /tmp
/Users/eunsong/FlashScale/scripts/verify.sh
```

실패 전파는 임시 작업 복사본에서 의도적으로 포맷 오류를 만든 뒤 `verify.sh`가 0이 아닌 종료 코드를 반환하는지 확인한다. 원본 작업 트리는 변경하지 않는다.

## 예상 변경 파일

- `docs/tasks/day-03-verification-harness.md`
- `ticketing-api/build.gradle`
- `ticketing-api/config/checkstyle/checkstyle.xml`
- `predictor/requirements-test.txt`
- `predictor/pyproject.toml`
- `scripts/verify.sh`
- `README.md`
- `docs/retrospectives/day-03.md`

## 위험 요소

- 새 검증 도구의 기본 규칙이 기존 부트스트랩 코드와 충돌할 수 있다. Day 3에 필요한 최소 설정만 적용하고 규칙을 통과시키기 위한 무관한 리팩터링은 하지 않는다.
- Gradle과 Python 의존성이 준비되지 않은 환경에서는 최초 검증에 네트워크가 필요할 수 있다. 필요한 설치 명령을 README에 명시한다.
- 검증 스크립트가 호출 위치에 의존하면 하위 디렉터리나 저장소 외부에서 실패할 수 있다. 스크립트 자신의 위치를 기준으로 저장소 루트를 계산한다.

## 완료 결과

- Day 2의 Spring 및 Python 기본 테스트를 먼저 실행해 부트스트랩이 정상임을 확인했다.
- Spring에 Spotless/Google Java Format 포맷 검사와 Checkstyle 정적 분석을 구성했다.
- Spring Actuator `/actuator/health`의 HTTP 200과 `UP` 응답을 MockMvc로 자동 검증하도록 보강했다.
- Python에 Ruff 포맷 검사와 lint 설정을 구성했다.
- 호출 위치와 무관하게 저장소 루트를 계산하고 모든 검사를 순서대로 실행하는 `scripts/verify.sh`를 추가했다.
- 루트와 `/tmp`에서 통합 검증 성공을 확인했다.
- 임시 복사본에 Python 포맷 오류를 만든 검증에서 `verify.sh`가 exit code 1을 반환하는 것을 확인하고 임시 복사본을 삭제했다.
- README에 준비·개별·전체 검증 명령과 도구 선택 이유 및 대안을 기록했다.
