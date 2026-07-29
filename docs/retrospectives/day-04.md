# Day 4 회고

## 오늘의 목표

- Pull Request가 생성되거나 변경될 때 GitHub Actions에서 자동 검증한다.
- 로컬과 CI가 루트 `./scripts/verify.sh`를 같은 단일 진입점으로 사용한다.
- 프로젝트에 맞는 Java 17과 Python 3.10 환경을 준비한다.
- 최소 권한과 dependency cache를 적용한다.
- Docker, 배포, 도메인 기능 등 Day 4 범위 밖 기능은 추가하지 않는다.

## 완료한 작업

- Pull Request와 수동 실행을 지원하는 `.github/workflows/ci.yml`을 추가했다.
- Java 17 Temurin과 Python 3.10을 설치하도록 구성했다.
- Gradle 및 pip dependency cache를 각 프로젝트의 의존성 설정 파일과 연결했다.
- CI에서 `predictor/.venv`를 만들고 `requirements-test.txt`를 설치하도록 구성했다.
- workflow 권한을 `contents: read`로 제한하고 checkout credential 유지를 비활성화했다.
- 검증 단계가 별도 명령을 중복하지 않고 `./scripts/verify.sh`만 실행하도록 구성했다.
- `scripts/verify.sh`의 로컬 실행 권한과 Git mode를 확인했다.
- Day 4 전용 브랜치와 Draft Pull Request #5를 만들고 `Verify` job이 실제로 통과함을 확인했다.
- Pull Request 제목과 설명의 언어 및 순서를 통일하기 위해 공통 한글 PR 템플릿을 추가했다.
- Pull Request가 잘못된 브랜치로 병합되는 문제를 막기 위해 base 확인 규칙과 체크리스트를 추가했다.
- Day 4 acceptance criteria를 실제 구현 및 검증 결과에 따라 갱신했다.

## 이해한 개념

- GitHub Actions의 `on`은 workflow를 시작할 이벤트를 정의한다. `pull_request`는 PR 변경을 자동 검증하고, `workflow_dispatch`는 사람이 필요할 때 같은 workflow를 수동 실행하게 한다.
- workflow의 `permissions`는 `GITHUB_TOKEN`이 할 수 있는 일을 제한한다. 코드 읽기와 검증만 필요한 CI에는 `contents: read`면 충분하다.
- setup action은 단순 설치뿐 아니라 tool cache와 dependency cache 연결을 담당한다. Java와 Python 버전을 명시하면 runner 이미지가 바뀌어도 프로젝트 실행 환경을 더 일관되게 유지할 수 있다.
- dependency cache는 이미 내려받은 패키지를 재사용해 설치 시간을 줄이지만 가상환경이나 설치 완료 상태 자체를 보장하지 않는다. 따라서 cache가 있어도 의존성 설치 명령은 매번 실행해야 한다.
- CI와 로컬이 같은 검증 스크립트를 호출하면 검사 목록을 workflow에 다시 작성하지 않아도 된다. 검증 규칙 변경도 `verify.sh` 한 곳에서 관리할 수 있다.
- `verify.sh`의 `set -eu`와 workflow의 기본 shell 실패 처리 덕분에 하위 검사 하나의 실패가 스크립트와 job의 실패로 연속 전파된다.
- 후속 Day 브랜치를 이전 Day 브랜치에서 만들 때 PR base도 이전 Day 브랜치로 지정하면 현재 Day의 diff만 독립적으로 검토할 수 있다.
- PR 템플릿은 구현 파일이 아니지만 변경 내용과 검증 근거를 같은 순서로 남겨 리뷰어의 탐색 비용을 줄이는 협업 인터페이스다.
- stacked PR은 중간 브랜치를 base로 삼아 diff를 작게 만들 수 있지만, 최종 병합 흐름과 사용자의 의도를 바꾸므로 명시적인 승인 없이 선택하면 안 된다.

## 막힌 부분과 해결 과정

- 사용자가 지정한 `docs/templates/TASK_TEMPLATE.md`는 존재하지 않았다. 저장소를 검색해 실제 템플릿인 `docs/tasks/TASK_TEMPLATE.md`를 확인하고 그 구조를 사용했다.
- 최초 로컬 `./scripts/verify.sh` 실행은 사용자 Gradle cache의 lock 파일에 대한 샌드박스 권한 오류로 실패했다. 승인된 환경에서 같은 명령을 다시 실행해 코드 문제가 아님을 구분하고 전체 검증 통과를 확인했다.
- 처음에는 Day 3 브랜치에서 작업 파일을 만들었다. 커밋 전에 발견했기 때문에 변경을 잃지 않고 `agent/day-04-pull-request-ci` 브랜치를 새로 만들어 옮겼다.
- 최초 Draft PR은 기존 한글 관례와 달리 영어 제목과 영어 섹션명을 사용했다. 공통 템플릿을 한글로 추가하고 기존 PR도 같은 형식으로 수정해 재발 가능성을 줄였다.
- Day 4 PR의 diff만 작게 보이게 하려고 Day 3 브랜치를 base로 선택했지만 최종 병합 대상이 `main`이어야 한다는 요구를 놓쳤다. 잘못된 PR은 사용자가 revert했고, 원격 상태와 `origin/main...HEAD` diff를 다시 확인한 뒤 `main` 대상 PR을 새로 생성하도록 바로잡았다.
- 새 브랜치 생성 시 `.git` 참조 쓰기가 샌드박스에서 제한되었다. 승인된 Git 명령으로 브랜치를 생성했다.
- PR 생성 전 GitHub CLI 토큰이 만료되어 인증에 실패했다. 기기 인증으로 `eschoi04` 계정을 다시 연결한 뒤 push와 Draft PR 생성을 완료했다.
- 로컬에서는 GitHub의 `pull_request` 이벤트와 runner 환경을 완전히 재현할 수 없다. Draft PR #5를 실제로 생성하고 `Verify` job이 1분 7초 만에 통과하는 것을 확인했다.

## Codex가 제안했지만 채택하지 않은 내용

- 여러 Java 또는 Python 버전을 검사하는 CI matrix는 현재 프로젝트가 Java 17과 Python 3.10 기준 하나를 명확히 갖고 있어 불필요하므로 추가하지 않았다.
- Gradle, Ruff, pytest 명령을 workflow에 각각 작성하는 방식은 로컬 검증과 중복되어 시간이 지나면 두 경로가 달라질 수 있으므로 채택하지 않았다.
- Python 가상환경 전체를 cache하는 방식은 환경 경로와 설치 상태를 숨길 수 있어 사용하지 않고 pip 다운로드 cache만 적용했다.
- Python 의존성 버전 고정과 lock file 도입은 재현성을 개선하지만 Day 4 CI 구축 범위를 넘으므로 후속 작업으로 남겼다.
- branch protection의 required status check 설정은 저장소 운영 정책 변경이므로 workflow 구현에 포함하지 않았다.
- Docker, PostgreSQL, GHCR, 배포 workflow와 Kubernetes 구성은 프로젝트 v1에는 포함되지만 Day 4 제외 범위이므로 추가하지 않았다.

## 검증 결과

- workflow YAML 구문을 파싱해 문법 오류가 없음을 확인했다.
- `pull_request`, `workflow_dispatch`, `contents: read`, Java 17, Python 3.10, Gradle/pip cache 설정을 정적으로 확인했다.
- workflow 안에서 `run: ./scripts/verify.sh`가 정확히 한 번만 등장하고 `continue-on-error`나 matrix가 없음을 확인했다.
- `scripts/verify.sh`의 파일 시스템 권한이 `755`, Git mode가 `100755`임을 확인했다.
- 로컬 `./scripts/verify.sh`에서 Spring 포맷, Checkstyle, 테스트가 통과했다.
- 같은 로컬 검증에서 Ruff 포맷, lint와 Python 테스트 1개가 통과했다.
- Draft Pull Request #5에서 `Verify` job이 자동 시작되어 1분 7초 만에 통과했다.
- 공통 PR 템플릿의 네 제목이 변경 사항, 변경 이유, 영향, 검증 순서로 정확히 배치되었음을 확인했다.
- Draft Pull Request #5의 제목과 본문을 한글 형식으로 수정하고 네 섹션 순서를 템플릿과 일치시켰다.
- 후속 `Verify` 실행 로그에서 Gradle wrapper와 dependency cache, pip cache가 모두 복원되었음을 확인했다.
- PR 생성 직전에 원격 기본 브랜치가 `main`인지 확인하고 `origin/main...HEAD` 변경 파일이 Day 4 범위와 일치함을 확인했다.
- `git diff --check`가 통과했다.

## 남은 위험

- Python 의존성 버전이 고정되지 않아 외부 패키지 릴리스에 따라 같은 커밋의 설치 결과가 달라질 수 있다.
- `ubuntu-latest` runner 이미지와 major tag로 지정한 GitHub Action 구현은 GitHub가 업데이트할 수 있다.
- `workflow_dispatch`는 workflow가 기본 브랜치에 병합된 후 Actions UI에서 최종 확인해야 한다.
- `Verify`가 통과해도 branch protection에 required status check로 지정하지 않으면 실패한 PR의 병합을 GitHub가 자동으로 차단하지 않는다.

## 내일 첫 번째 작업

- Day 4 PR의 base인 Day 3 PR 병합 상태와 `Verify` 결과를 확인한다. 순서대로 병합한 뒤 기본 브랜치에서 수동 실행과 cache 복원 여부를 확인하고, 다음 활성 task의 범위를 템플릿으로 정리한다.
