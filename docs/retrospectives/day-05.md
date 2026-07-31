# Day 5 회고

## 오늘의 목표

- `ticketing-api`와 `predictor`에 각각 멀티스테이지 Dockerfile을 작성한다.
- 두 이미지의 최종 컨테이너를 비-root 사용자로 실행한다.
- 기존 health endpoint를 이용한 Docker `HEALTHCHECK`를 추가한다.
- 애플리케이션별 `.dockerignore`로 불필요한 빌드 컨텍스트를 제외한다.
- 두 이미지를 실제 빌드하고 독립 컨테이너로 실행해 외부 health 응답, Docker health 상태와 실행 사용자를 검증한다.
- 기존 CI와 `scripts/verify.sh`의 책임을 변경하지 않는다.
- Docker Compose, PostgreSQL, 컨테이너 간 통신, GHCR, GitHub Actions 수정, Kubernetes, 도메인 로직 및 ML 모델은 추가하지 않는다.

## 완료한 작업

- 최신 `origin/main`을 기준으로 `agent/day-05-containerization` 브랜치를 준비했다.
- 실제 Gradle wrapper, Java 버전, Python 의존성 관리 방식, 모듈 경로, 포트와 health endpoint를 확인하고 Day 5 task를 작성했다.
- `ticketing-api`에 Temurin Java 17 JDK 빌드 스테이지와 JRE 실행 스테이지를 분리한 Dockerfile을 추가했다.
- Spring 빌드 스테이지에서 저장소의 Gradle wrapper 9.5.1로 `bootJar`를 실행하고 최종 이미지에는 실행 JAR만 복사했다.
- `predictor`에 pip 런타임 의존성 설치 스테이지와 Python 실행 스테이지를 분리한 Dockerfile을 추가했다.
- Predictor가 개발용 reload 없이 `app.main:app`을 Uvicorn으로 실행하도록 구성했다.
- 두 최종 이미지에 시스템 사용자와 그룹 `app`을 만들고 `USER app`을 지정했다.
- Spring은 기존 `/actuator/health`, Predictor는 기존 `/health`를 호출하는 Docker `HEALTHCHECK`를 추가했다.
- Gradle 캐시·빌드 결과, Python 가상환경·캐시, 테스트 파일, IDE 및 VCS 메타데이터를 제외하는 애플리케이션별 `.dockerignore`를 추가했다.
- 두 이미지를 실제 빌드하고 호스트 포트 `18080`과 `18000`에 각각 독립 컨테이너로 실행했다.
- 외부 health 응답, Docker의 `healthy` 상태, 이미지 실행 사용자와 실행 중 UID를 확인했다.
- Day 5 acceptance criteria를 실제 검증 결과에 따라 모두 완료 표시했다.
- 검증용 컨테이너를 중지하고 제거했으며 재사용 가능한 로컬 이미지는 남겼다.

## 이해한 개념

- 멀티스테이지 빌드는 컴파일과 의존성 설치에 필요한 도구를 빌드 스테이지에 두고, 최종 실행 스테이지에는 실행 산출물과 런타임만 복사해 이미지의 책임과 공격 표면을 줄인다.
- Spring에서는 Java 코드를 컴파일하고 JAR를 만들기 위해 JDK가 필요하지만, 완성된 JAR를 실행할 때는 JRE만 있으면 된다.
- Docker 빌드 안에서도 저장소의 Gradle wrapper를 사용하면 호스트에 설치된 Gradle에 의존하지 않고 프로젝트가 지정한 Gradle 9.5.1로 같은 빌드를 수행할 수 있다.
- Predictor의 빌드 스테이지에서 `pip --prefix=/install`로 런타임 의존성을 한 경로에 모으면 최종 스테이지에 필요한 패키지만 명확하게 복사할 수 있다.
- `.dockerignore`는 Git 추적 여부가 아니라 Docker daemon에 전송할 빌드 컨텍스트를 제어한다. 로컬 가상환경이나 캐시를 제외하면 전송량을 줄이고 호스트 운영체제의 산출물이 Linux 이미지에 섞이는 것을 막는다.
- `EXPOSE`는 이미지가 사용하는 포트를 설명하는 메타데이터이며 호스트 포트를 자동으로 게시하지 않는다. 외부 접근에는 `docker run --publish 호스트포트:컨테이너포트`가 별도로 필요하다.
- 컨테이너가 `Up`인 것과 애플리케이션이 요청을 정상 처리하는 것은 다르다. Docker `HEALTHCHECK`는 컨테이너 내부 상태를 반복 확인하고, 호스트의 `curl`은 실제 포트 게시와 외부 접근 경로까지 검증한다.
- Dockerfile의 `USER app`은 이미지의 기본 실행 사용자를 정하고, 실행 중 `id -u` 확인은 설정이 실제 프로세스에도 적용되었음을 검증한다. UID `0`은 root이므로 오늘 확인한 UID `999`는 비-root 실행이다.
- exec 형식의 `ENTRYPOINT`를 사용하면 애플리케이션 프로세스가 컨테이너의 주 프로세스로 실행되어 Docker 종료 신호를 직접 받을 수 있다.

## 막힌 부분과 해결 과정

- 처음 Day 5 브랜치를 로컬의 오래된 `origin/main`에서 생성해 Day 4 CI 파일이 보이지 않았다. GitHub의 실제 PR #7 병합 상태와 원격 `main` 커밋을 확인한 결과 Day 4는 정상 병합됐고 로컬 원격 추적 참조만 갱신되지 않은 상태였다. `git fetch origin` 후 로컬 `main`을 fast-forward하고, 아직 변경이 없던 Day 5 브랜치를 최신 `main`에서 다시 만들었다.
- 사용자가 지정한 `docs/templates/TASK_TEMPLATE.md`는 존재하지 않았다. 저장소를 검색해 실제 템플릿인 `docs/tasks/TASK_TEMPLATE.md`를 사용했다.
- Docker CLI는 설치되어 있었지만 Docker daemon이 실행 중이 아니었다. Docker Desktop을 시작하고 daemon이 `linux/aarch64` 환경으로 준비된 것을 확인한 뒤 빌드를 진행했다.
- 최초 Spring 이미지 빌드는 `eclipse-temurin:17-jdk-alpine` 태그에 현재 ARM64용 manifest가 없어 기반 이미지 해석 단계에서 실패했다. Java 17을 유지하면서 ARM64와 AMD64를 지원하는 `eclipse-temurin:17-jdk-jammy`와 `17-jre-jammy`로 변경해 실제 빌드를 통과시켰다.
- Spring 최종 이미지에 health check용 `curl` 설치 단계를 작성했다. 현재 Temurin JRE Jammy 이미지에도 `curl`이 포함되어 있었지만, 향후 같은 기반 이미지 태그의 구성이 바뀌더라도 Dockerfile이 health check 도구의 존재를 직접 보장해야 한다는 검토 의견을 반영해 명시적 설치를 유지했다.
- 최초 `./scripts/verify.sh` 실행은 사용자 Gradle cache lock 파일에 대한 샌드박스 접근 제한으로 실패했다. 동일 명령을 승인된 환경에서 다시 실행해 코드 결함이 아님을 구분하고 전체 검증 통과를 확인했다.

## Codex가 제안했지만 채택하지 않은 내용

- Docker Compose는 두 컨테이너를 함께 실행하기 편하지만 이번 Day 5 제외 범위이므로 추가하지 않고 각각 `docker run`으로 독립 실행을 검증했다.
- PostgreSQL 연결과 컨테이너 간 네트워크 구성은 향후 통합 실행에 필요하지만 이번 작업은 애플리케이션별 이미지의 독립 빌드와 실행에 집중했으므로 추가하지 않았다.
- 기존 GitHub Actions에 Docker 이미지 빌드 검증을 추가하면 원격 재현성을 높일 수 있지만 기존 CI 책임을 변경하지 말라는 요구에 따라 workflow를 수정하지 않았다.
- Predictor 이미지에 `curl` 같은 별도 HTTP 클라이언트를 설치하는 방식은 health check만을 위한 의존성과 레이어가 늘어나므로 Python 표준 라이브러리 `urllib.request`를 사용했다.
- Python lock file이나 버전 고정을 도입하면 이미지 재현성이 좋아지지만 기존 의존성 관리 정책을 바꾸는 별도 결정이 필요해 이번 범위에서는 적용하지 않았다.
- 이미지 레지스트리 게시, 취약점 스캔, Kubernetes 배포는 운영 완성도를 높일 수 있지만 GHCR과 Kubernetes가 명시적 제외 범위이고 스캔 정책도 아직 정해지지 않아 추가하지 않았다.

## 검증 결과

- `docker build --tag flashscale-ticketing-api:day-05 ./ticketing-api`가 성공했다.
- `docker build --tag flashscale-predictor:day-05 ./predictor`가 성공했다.
- Spring 최종 이미지 크기는 111,878,714 bytes, Predictor 최종 이미지 크기는 49,651,487 bytes였다.
- `flashscale-ticketing-api-day-05`를 `18080:8080`, `flashscale-predictor-day-05`를 `18000:8000`으로 실행했다.
- 호스트에서 `http://localhost:18080/actuator/health`를 호출해 `UP` 상태를 확인했다.
- 호스트에서 `http://localhost:18000/health`를 호출해 `{"status":"UP"}` 응답을 확인했다.
- Docker가 보고한 두 컨테이너의 health 상태가 모두 `healthy`임을 확인했다.
- 두 이미지의 `Config.User`가 모두 `app`이고, 실행 중 `id -u` 결과가 모두 `999`임을 확인했다.
- `./scripts/verify.sh`에서 Spring Spotless, Checkstyle, 테스트가 모두 통과했다.
- 같은 전체 검증에서 Python Ruff 포맷, lint와 pytest가 통과했고 테스트 1개가 성공했다.
- 기존 `.github/workflows/ci.yml`과 `scripts/verify.sh`에 변경이 없음을 확인했다.
- `git diff --check`가 통과했고 Day 5 task에 미완료 acceptance checklist가 없음을 확인했다.
- 검증용 컨테이너 두 개를 중지하고 제거했다.

## 남은 위험

- `requirements.txt`의 Python 의존성 버전이 고정되어 있지 않아 빌드 시점에 따라 직접·간접 의존성 버전이 달라질 수 있다.
- Docker 기반 이미지 태그를 digest로 고정하지 않아 동일 태그의 기반 이미지가 갱신되면 이미지 내용과 취약점 상태가 달라질 수 있다.
- 오늘 실제 빌드와 실행은 로컬 ARM64 Docker daemon에서 수행했다. 선택한 기반 이미지는 AMD64 manifest도 제공하지만 AMD64 컨테이너 실행은 직접 검증하지 않았다.
- 이미지 취약점 스캔과 SBOM 생성은 수행하지 않았다.
- Docker 이미지 빌드는 기존 GitHub Actions의 검증 책임에 포함되지 않으므로 현재는 로컬에서만 검증된다.
- health check는 현재의 단순 부트스트랩 상태를 확인한다. 향후 데이터베이스 연결이 추가될 때 readiness가 어떤 외부 의존성을 반영해야 하는지 별도로 결정해야 한다.

## 내일 첫 번째 작업

- Day 5 변경 범위와 검증 결과를 다시 확인해 커밋하고 `origin/main...HEAD` diff가 현재 task와 일치하는지 점검한다. 이후 다음 활성 task의 요구사항을 `TASK_TEMPLATE.md`로 정리하고 사용자 확인을 받은 뒤 작업을 시작한다.
