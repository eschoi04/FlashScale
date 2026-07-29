# Task: Day 4 Pull Request CI

## 목적

Pull Request마다 GitHub Actions에서 저장소 루트의 `./scripts/verify.sh`를 실행해 Spring Boot 애플리케이션과 Python 예측 컴포넌트의 품질 검사를 자동화한다. 로컬과 CI가 같은 검증 진입점을 사용하게 하여 검증 절차의 중복과 차이를 방지하고, 어느 검사든 실패하면 Pull Request에서 즉시 확인할 수 있게 한다.

## 작업 범위

- Pull Request에서 자동 실행되는 GitHub Actions workflow를 추가한다.
- 필요할 때 GitHub UI에서 같은 workflow를 수동 실행할 수 있게 한다.
- 프로젝트 설정에 맞춰 Java 17과 Python 3.10 환경을 준비한다.
- Gradle과 pip 의존성 cache를 적용한다.
- Predictor의 `.venv`를 만들고 테스트 의존성을 설치한다.
- workflow의 검증 단계에서 루트 `./scripts/verify.sh`만 호출한다.
- workflow 권한과 실행 파일 권한을 확인한다.
- 로컬 통합 검증을 실행하고 CI에서 추가로 확인할 항목을 기록한다.

## 제외 범위

- Dockerfile과 Docker Compose
- PostgreSQL
- GHCR
- 배포 workflow
- Kubernetes
- 도메인 기능 및 ML 기능
- 불필요한 CI matrix
- Python 의존성 버전 고정 및 lock file 도입
- 저장소 branch protection 설정 변경

## Acceptance Criteria

- [x] `.github/workflows/ci.yml`이 생성되었다.
- [x] Pull Request가 생성되거나 변경되면 workflow가 자동 실행된다.
- [x] `workflow_dispatch`로 수동 실행할 수 있다.
- [x] Java 17과 Python 3.10 환경이 준비된다.
- [x] Predictor의 `.venv`에 `requirements-test.txt` 의존성이 설치된다.
- [x] Gradle과 pip 의존성 cache가 프로젝트 설정 파일을 기준으로 적용된다.
- [x] workflow 권한이 `contents: read`로 제한된다.
- [x] 검증 명령을 중복하지 않고 `./scripts/verify.sh`를 단일 진입점으로 호출한다.
- [x] `verify.sh`가 실패하면 workflow도 실패하는 구성을 사용한다.
- [x] `scripts/verify.sh`가 로컬과 Git에서 실행 가능한 상태다.
- [x] 로컬 `./scripts/verify.sh`가 통과한다.
- [x] Day 4 제외 범위의 기능을 추가하지 않았다.
- [x] GitHub Actions에서 추가로 확인할 사항이 기록되었다.

## 검증 명령

```bash
./scripts/verify.sh
```

추가로 workflow 파일에서 이벤트, 권한, 런타임 버전, cache 설정, 단일 검증 진입점을 정적으로 확인한다. GitHub Actions의 실제 Pull Request 실행 결과와 cache 복원 여부는 workflow를 원격 브랜치에 push한 뒤 GitHub에서 확인한다.

## 예상 변경 파일

- `docs/tasks/day-04-pull-request-ci.md`
- `.github/workflows/ci.yml`

## 위험 요소

- `scripts/verify.sh`는 `predictor/.venv/bin/python`을 사용하므로 workflow가 같은 경로에 가상환경을 만들지 않으면 실패한다.
- Python 요구사항 파일에 의존성 버전이 고정되지 않아 외부 패키지의 최신 릴리스에 따라 같은 커밋의 설치 결과가 달라질 수 있다. Day 4에서는 범위를 확대하지 않고 후속 위험으로 남긴다.
- dependency cache는 다운로드를 줄이지만 의존성 설치 단계를 대신하지 않으므로 매 실행에서 설치 명령은 필요하다.
- GitHub Actions 이벤트와 cache 동작은 로컬에서 완전히 재현할 수 없다. Pull Request에서 실제 workflow 실행 결과를 별도로 확인해야 한다.
- branch protection의 필수 검사 지정은 저장소 설정 작업이므로 이번 task에서 변경하지 않는다.

## 완료 결과

- Pull Request와 수동 실행을 지원하는 `.github/workflows/ci.yml`을 추가했다.
- workflow 권한을 `contents: read`로 제한하고 checkout 단계에서 credential 유지를 비활성화했다.
- Java 17 Temurin과 Python 3.10을 준비하고 Gradle 및 pip cache가 각 의존성 설정 파일을 key 계산에 사용하도록 구성했다.
- `predictor/.venv`를 만든 뒤 `requirements-test.txt`를 설치하여 기존 `scripts/verify.sh`가 기대하는 로컬 경로와 CI 환경을 일치시켰다.
- 검증 단계는 별도 Gradle, Ruff, pytest 명령을 중복하지 않고 `./scripts/verify.sh`만 실행한다. 실패를 무시하는 설정이 없으므로 스크립트의 0이 아닌 종료 코드가 job 실패로 전파된다.
- `scripts/verify.sh`의 파일 시스템 권한 `755`와 Git mode `100755`를 확인했다.
- workflow YAML 구문, 이벤트, 최소 권한, Java/Python 버전, cache 설정 및 단일 검증 명령을 정적으로 확인했다.
- 로컬 `./scripts/verify.sh`에서 Spring 포맷·정적 분석·테스트, Python 포맷·lint·테스트가 모두 통과했다. Python 테스트는 1개가 통과했다.
- GitHub에서는 Pull Request 생성 또는 변경 시 `Verify` job이 자동 실행되는지, 수동 실행 버튼이 제공되는지, 두 번째 실행부터 Gradle과 pip cache가 복원되는지 확인해야 한다.
- CI를 병합 필수 조건으로 사용할 경우 저장소 branch protection에서 `Verify` check를 required status check로 별도 지정해야 한다.
- Python 의존성이 버전 고정되지 않은 위험은 Day 4 범위 밖의 후속 작업으로 남겼다.
