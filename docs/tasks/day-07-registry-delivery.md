# Task: Day 7 GHCR 이미지 빌드 및 게시 자동화

## 목적

`main` 브랜치에 변경사항이 반영될 때 GitHub Actions가 `ticketing-api`와
`predictor` Docker 이미지를 독립적으로 빌드하고 GHCR에 게시한다. 각 산출물을
workflow를 실행한 전체 commit SHA로 식별할 수 있게 해 향후 배포가 동일한 이미지를
정확히 참조할 수 있는 기반을 만든다.

## 작업 범위

- `.github/workflows/registry-delivery.yml`을 추가한다.
- workflow는 `main` 브랜치 push에만 자동 실행한다.
- `GITHUB_TOKEN` 권한은 `contents: read`, `packages: write`만 부여한다.
- GHCR 로그인은 `${{ github.actor }}`와 `${{ secrets.GITHUB_TOKEN }}`을 사용한다.
- `ticketing-api`는 `./ticketing-api` context와 해당 Dockerfile로 빌드한다.
- `predictor`는 `./predictor` context와 해당 Dockerfile로 빌드한다.
- 두 이미지 구성을 읽기 쉬운 matrix로 표현하고 cache scope를 서비스별로 분리한다.
- `GITHUB_REPOSITORY`를 소문자로 정규화해 유효한 전체 이미지 이름을 만든다.
- 두 이미지에 workflow의 전체 `${{ github.sha }}` 하나만 태그로 붙인다.
- 각 이미지에 source repository와 revision OCI label을 추가한다.
- 기존 CI의 액션 major-version 고정 방식과 job/step 명명 관례를 유지한다.
- 로컬 검증 결과와 `main` 반영 후 원격 검증 절차를 완료 결과에 기록한다.

## 제외 범위

- PostgreSQL 이미지 빌드 또는 GHCR 게시
- `latest`, `main`, 날짜, 버전, 짧은 SHA 태그
- PAT 또는 새로운 repository secret
- Docker Hub 및 다른 registry 게시
- 멀티 아키텍처 빌드
- GitHub Release, attestations 및 추가 권한
- Kubernetes manifest와 배포
- 애플리케이션, Dockerfile, Compose 및 기존 CI workflow 변경
- Spring/PostgreSQL 실제 데이터 연동, 도메인 API, migration, predictor 연동
- 로그인, 결제, 프론트엔드, Redis 및 Kafka

## Acceptance Criteria

- [x] Day 7 작업 문서가 템플릿에 맞게 작성됐다.
- [x] registry-delivery workflow가 `main` push만 감지한다.
- [x] `GITHUB_TOKEN`에 `contents: read`, `packages: write`만 부여했다.
- [x] GHCR 인증에 actor와 자동 생성 `GITHUB_TOKEN`을 사용한다.
- [x] `ticketing-api` 이미지가 올바른 context와 Dockerfile에서 빌드된다.
- [x] `predictor` 이미지가 올바른 context와 Dockerfile에서 빌드된다.
- [x] 두 이미지 이름 전체가 소문자로 정규화된다.
- [x] 두 이미지 모두 전체 commit SHA 하나만 태그로 사용한다.
- [x] 두 이미지에 source와 revision OCI label이 포함된다.
- [x] `latest` 및 그 밖의 추가 태그를 만들지 않는다.
- [x] PostgreSQL 이미지를 빌드하거나 게시하지 않는다.
- [x] 두 이미지의 GitHub Actions cache scope가 분리됐다.
- [x] 기존 `./scripts/verify.sh`가 통과한다.
- [x] 가능한 두 Dockerfile의 로컬 빌드 검증이 통과한다.
- [x] `docker compose config`가 통과한다.
- [x] workflow YAML과 GitHub Actions 구조 검토가 완료됐다.
- [x] `main` 반영 후 확인할 원격 검증 절차가 문서화됐다.
- [x] Day 7 회고가 작성됐다.

## 검증 명령

```bash
./scripts/verify.sh
docker build -t flashscale-ticketing-api:day7 ./ticketing-api
docker build -t flashscale-predictor:day7 ./predictor
docker compose config
git diff --check
```

저장소에 `actionlint`가 이미 설치되었거나 구성되어 있으면 workflow 정적 검사를
추가로 실행한다. 실제 GHCR push와 package metadata는 `main` 반영 후 원격에서
확인한다.

## 예상 변경 파일

- `.github/workflows/registry-delivery.yml`
- `docs/tasks/day-07-registry-delivery.md`
- `docs/retrospectives/day-07.md`

## 위험 요소

- 로컬 Docker daemon을 사용할 수 없으면 Dockerfile 빌드는 재현하지 못하며, 이를
  성공으로 처리하지 않고 미검증 항목으로 기록해야 한다.
- workflow 구문이 유효해도 GHCR 쓰기 권한과 package 연결은 `main` push 이후에만
  실제로 확인할 수 있다.
- matrix의 한 이미지 빌드 실패는 다른 matrix 실행과 독립적으로 표시되므로 두 실행
  결과를 모두 확인해야 한다.
- 저장소 또는 소유자 이름에 대문자가 있으면 Docker repository 이름이 유효하지
  않으므로 workflow 안에서 전체 `GITHUB_REPOSITORY`를 소문자로 바꿔야 한다.

## 완료 결과

- `.github/workflows/registry-delivery.yml`을 추가했다. `main` push만 trigger로
  선언하고 workflow 수준 권한은 `contents: read`, `packages: write`로 제한했다.
- 공식 Docker 문서의 현재 major 버전과 기존 CI의 major-version 고정 방식을 따라
  `actions/checkout@v6`, `docker/login-action@v4`,
  `docker/setup-buildx-action@v4`, `docker/build-push-action@v7`을 사용했다.
- matrix의 두 항목에 서비스별 context, Dockerfile, cache scope를 명시했다.
  `fail-fast: false`로 한 이미지의 실패가 진행 중인 다른 이미지 실행을 취소하지
  않게 했다.
- `tr '[:upper:]' '[:lower:]'`로 `owner/repository` 전체를 소문자로 바꾸고,
  서비스 suffix와 전체 `${{ github.sha }}`를 결합해 이미지 태그를 하나만 만든다.
- source repository와 전체 revision을 OCI label로 추가했다. PostgreSQL, 추가 태그,
  PAT, 사용자 secret, 멀티 아키텍처, attestation은 추가하지 않았다.
- `./scripts/verify.sh`, `docker compose config --quiet`, Ruby YAML parsing,
  `git diff --check`가 통과했다. 저장소에 `actionlint`가 없어 실행하지 않았고 새
  전역 도구도 설치하지 않았다.
- 최초 두 `docker build` 명령은 Docker daemon이 꺼져 있어 실패했다. Docker
  Desktop을 시작한 뒤 동일한 명령을 다시 실행했고 `ticketing-api`와 `predictor`
  이미지가 각각 `flashscale-ticketing-api:day7`,
  `flashscale-predictor:day7`로 성공적으로 빌드됐다.
- `main` 반영 후 GitHub 저장소의 Actions 탭에서 `Registry Delivery` run이 push
  commit으로 한 번 시작됐는지 확인한다. `Build and push ticketing-api image`와
  `Build and push predictor image` matrix 실행이 모두 성공했는지 확인하고, 각
  build step의 context와 push 결과를 검토한다.
- GitHub Packages 또는 GHCR에서 다음 두 package/version을 확인한다.
  `ghcr.io/<소문자-owner>/<소문자-repository>-ticketing-api:<전체-SHA>`와
  `ghcr.io/<소문자-owner>/<소문자-repository>-predictor:<전체-SHA>`가 존재해야
  한다. 각 manifest의 source URL과 revision label이 해당 저장소와 push commit을
  가리키는지, `latest`나 PostgreSQL package가 생기지 않았는지도 확인한다.
- 원격 검증 전까지 남은 위험은 실제 `GITHUB_TOKEN`의 package write 동작, GHCR의
  저장소 연결, GitHub-hosted runner에서의 두 이미지 빌드와 push 성공 여부를 아직
  증명하지 못했다는 점이다.
