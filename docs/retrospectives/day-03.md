# Day 3 회고

## 오늘의 목표

- Day 2 부트스트랩 상태를 먼저 확인하고 결함이 있으면 선행 수정한다.
- Spring과 Python의 포맷, 정적 분석/lint, 테스트를 자동 검증한다.
- 루트 `scripts/verify.sh` 하나로 전체 검증을 실행한다.
- 호출 위치 독립성과 실패 종료 코드 전파를 확인한다.
- DB, Docker, CI 등 Day 3 범위 밖 기능은 추가하지 않는다.

## 완료한 작업

- Day 2 Spring 테스트와 Python 테스트가 통과함을 확인했다.
- Spring에 Spotless/Google Java Format과 Checkstyle을 구성했다.
- Checkstyle에 import, 빈 줄, 중괄호 관련 최소 정적 규칙을 적용했다.
- Python 테스트 의존성에 Ruff를 추가하고 Python 3.10 기준 포맷/lint 설정을 작성했다.
- 양쪽 애플리케이션의 포맷, 정적 분석/lint, 테스트를 차례로 실행하는 `scripts/verify.sh`를 작성했다.
- README에 개발 환경 준비, 개별 검증, 전체 검증, 도구 선택 이유와 대안을 기록했다.
- Day 3 acceptance criteria를 실제 검증 결과에 따라 모두 완료 표시했다.

## 이해한 개념

- 포맷 검사는 코드 표현을 일관되게 유지하고, lint/static analysis는 잠재 오류나 유지보수 규칙을 검사하므로 역할이 다르다.
- 셸의 `set -e`는 하위 명령 실패 시 즉시 종료해 실패 코드를 호출자에게 전달하고, `set -u`는 준비되지 않은 변수 사용을 실패로 처리한다.
- 실행 시점의 현재 작업 디렉터리가 아니라 스크립트 파일의 디렉터리를 기준으로 경로를 계산해야 어디서 호출해도 같은 저장소를 검증한다.
- 통합 스크립트의 성공 경로뿐 아니라 의도적인 오류가 0이 아닌 종료 코드로 전파되는 실패 경로도 별도로 확인해야 한다.

## 막힌 부분과 해결 과정

- 최초 Spring 테스트는 사용자 Gradle 캐시의 lock 파일에 대한 샌드박스 권한 오류로 실패했다. 같은 명령을 승인된 권한으로 다시 실행해 코드 결함이 아님을 구분하고 테스트 통과를 확인했다.
- Ruff formatter는 기존 Python 파일을 포맷된 상태로 판단했지만 import 정렬 lint가 모듈 수준 코드 앞의 빈 줄을 지적했다. Ruff의 안전한 자동 수정을 적용한 뒤 formatter와 lint가 함께 통과함을 확인했다.
- 최초 Spotless 실행에는 플러그인 다운로드와 Gradle 캐시 접근이 필요했다. 승인된 권한으로 도구를 준비한 후 기존 Java 파일에 포맷만 적용했다.

## Codex가 제안했지만 채택하지 않은 내용

- Python 포맷과 lint에 Black과 Flake8을 각각 추가하는 방법은 의존성과 설정 지점이 늘어나므로 Ruff 하나로 두 역할을 수행했다.
- Spring 정적 분석에 SpotBugs를 추가하는 방법은 바이트코드 결함 분석에 유용하지만 Day 3 최소 부트스트랩보다 범위가 크므로 Checkstyle만 적용했다.
- IDE 포맷 설정만 공유하는 방법은 자동 검사와 실패 종료 코드 제공이 어려워 채택하지 않았다.
- DB, Docker, CI 설정은 프로젝트 v1에는 포함되지만 현재 Day 3 task의 제외 범위이므로 추가하지 않았다.

## 검증 결과

- Day 2 확인: `ticketing-api`의 `./gradlew test`와 `predictor`의 `.venv/bin/python -m pytest`가 통과했다.
- Spring: `./gradlew spotlessCheck checkstyleMain checkstyleTest test`가 통과했다.
- Python: Ruff formatter 검사, Ruff lint, pytest가 모두 통과했다.
- 저장소 루트에서 `./scripts/verify.sh`가 통과했다.
- `/tmp`에서 절대 경로로 `scripts/verify.sh`를 실행해 작업 디렉터리 독립성을 확인했다.
- 임시 복사본에 의도적인 Python 포맷 오류를 넣었을 때 스크립트가 Ruff 단계에서 exit code 1로 종료했다.
- `git diff --check`가 통과했다.

## 남은 위험

- Python 의존성은 버전이 고정되어 있지 않아 설치 시점에 따라 Ruff를 포함한 도구 버전이 달라질 수 있다.
- 검증 스크립트는 프로젝트 표준 로컬 환경인 macOS/Linux 계열의 POSIX shell과 `predictor/.venv` 경로를 전제로 한다.
- Checkstyle 규칙은 현재 부트스트랩에 필요한 최소 집합이며, 비즈니스 코드가 늘어날 때 규칙 강화 여부를 별도 task에서 검토해야 한다.

## 내일 첫 번째 작업

- `AGENTS.md`, 프로젝트 차터와 다음 활성 task를 읽고 Day 4 범위를 확인한 뒤, Day 3의 `./scripts/verify.sh`를 가장 먼저 실행해 기준 상태를 확인한다.
