# Day 7 회고

## 오늘의 목표

`main`에 반영된 commit을 기준으로 `ticketing-api`와 `predictor` 이미지를 각각
빌드하고, 전체 commit SHA 태그로 GHCR에 게시하는 GitHub Actions workflow를 만든다.

## 완료한 작업

- Day 6이 병합된 최신 `origin/main`에서 Day 7 전용 브랜치를 만들었다.
- `main` push 전용 `Registry Delivery` workflow를 추가했다.
- 최소 `GITHUB_TOKEN` 권한과 자동 생성 token을 사용하는 GHCR 로그인을 구성했다.
- 두 애플리케이션의 context, Dockerfile, cache scope를 matrix로 명시했다.
- 저장소 전체 이름을 소문자로 정규화하고 전체 commit SHA만 이미지 태그로 썼다.
- source와 revision OCI label을 이미지에 포함했다.
- 로컬 품질 게이트와 정적 검증을 수행하고 원격 검증 절차를 문서화했다.

## 이해한 개념

- GitHub의 저장소와 소유자 이름에는 대문자가 있을 수 있지만 Docker repository
  이름은 소문자여야 한다. 따라서 suffix만이 아니라 `owner/repository` 전체를
  정규화해야 한다.
- `${{ github.sha }}`는 workflow trigger가 가리키는 전체 commit SHA다. 이를 그대로
  태그로 쓰면 배포 산출물과 소스 commit 사이에 변경되지 않는 일대일 연결이 생긴다.
- `contents: read`는 checkout에, `packages: write`는 GHCR push에 필요하다. 이번
  workflow에는 attestation이나 OIDC가 없으므로 다른 권한은 필요하지 않다.
- matrix는 두 서비스의 반복되는 절차를 한 job 정의로 유지하면서도 각 항목을 별도
  job 실행으로 보여준다. `fail-fast: false`는 한쪽 실패가 다른 쪽 검증 기회를
  없애지 않도록 한다.
- BuildKit의 GitHub Actions cache는 scope가 같으면 서로의 cache record를 덮을 수
  있다. 서비스별 scope는 서로 다른 Dockerfile과 context의 cache를 분리한다.

## 막힌 부분과 해결 과정

- 로컬 Docker daemon socket이 없어 두 이미지 빌드를 실행하지 못했다. 실패를
  통과로 해석하지 않고 처음에는 task 체크리스트와 완료 결과에 미검증 상태로
  남겼다. Docker Desktop을 시작한 뒤 권한이 허용된 환경에서 동일한 명령을 다시
  실행해 두 빌드가 모두 성공한 것을 확인하고 체크리스트를 갱신했다.
- 저장소에 `actionlint`가 없었다. 이번 task를 위해 전역 도구를 설치하지 말라는
  요구에 따라 Ruby YAML parser와 수동 구조 검토로 대체했다.
- 시스템 Ruby의 Psych 버전이 `YAML.load_file`의 `aliases:` keyword를 지원하지
  않았다. alias를 사용하지 않는 workflow이므로 호환되는 기본 호출로 다시 검사해
  YAML parsing 성공을 확인했다.
- 처음 사용한 Bash의 `${VAR,,}` 소문자 확장은 GitHub Ubuntu에서는 지원되지만
  macOS 기본 Bash에서는 재현되지 않았다. POSIX 도구인 `tr`로 바꿔 같은 정규화를
  셸 버전에 덜 의존하도록 만들었다.
- 최초 step id에 하이픈을 사용했지만 expression의 점 표기법을 더 명확하게 만들기
  위해 `image_name`으로 바꿨다.

## Codex가 제안했지만 채택하지 않은 내용

- `latest`, branch, 날짜 및 짧은 SHA 태그는 편리할 수 있지만 이번 산출물의
  불변성과 명시적 제외 범위를 해치므로 추가하지 않았다.
- metadata action, attestation, QEMU와 reusable workflow는 현재 요구에 필요하지
  않고 권한 또는 복잡성을 늘리므로 추가하지 않았다.
- PostgreSQL은 공식 이미지를 직접 사용하므로 matrix에 포함하지 않았다.

## 검증 결과

- `./scripts/verify.sh`: 통과
- `docker compose config --quiet`: 통과
- Ruby를 사용한 workflow YAML parsing: 통과
- `git diff --check`: 통과
- `actionlint`: 저장소와 실행 환경에 없어 미실행
- `docker build -t flashscale-ticketing-api:day7 ./ticketing-api`: 재실행 통과
- `docker build -t flashscale-predictor:day7 ./predictor`: 재실행 통과
- `docker info --format '{{.ServerVersion}}'`: 통과, Docker Engine 29.2.1 확인
- 실제 GHCR push와 OCI metadata 확인: `main` 반영 전이므로 미실행

## 남은 위험

- `main` push에서 두 matrix 실행이 실제로 이미지를 빌드하고 GHCR에 쓸 수 있는지
  원격 검증이 필요하다.
- 로컬 Docker 빌드는 통과했지만 GitHub-hosted runner의 깨끗한 cache와 네트워크
  환경에서도 같은 결과가 나오는지는 workflow 실행으로 확인해야 한다.
- repository Actions 또는 package 설정이 `GITHUB_TOKEN` 쓰기를 별도로 제한하면
  workflow 권한 선언이 올바르더라도 push가 실패할 수 있다.

## 내일 첫 번째 작업

Day 7 변경이 `main`에 반영되면 먼저 Actions의 두 matrix 실행과 GHCR의 두 전체-SHA
태그를 확인하고, 검증 결과를 기록한 뒤 다음 활성 task를 정의한다.
