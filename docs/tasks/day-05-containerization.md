# Task: Day 5 애플리케이션 컨테이너화

## 목적

FlashScale의 Spring Boot `ticketing-api`와 Python `predictor`를 각각 독립적인 Docker 이미지로 빌드하고 실행할 수 있게 한다. 각 이미지는 빌드 도구와 런타임을 분리한 멀티스테이지 구조를 사용하고, 비-root 사용자 및 애플리케이션의 기존 health endpoint를 이용한 Docker health check를 적용해 최소한의 실행 안전성과 운영 상태 확인 기반을 마련한다.

실제 프로젝트 설정을 기준으로 `ticketing-api`는 Java 17, Gradle wrapper 9.5.1, 기본 포트 `8080`, `/actuator/health`를 사용한다. `predictor`는 pip의 `requirements.txt`, Uvicorn 모듈 경로 `app.main:app`, 기본 포트 `8000`, `/health`를 사용한다.

## 작업 범위

- `ticketing-api`에 Gradle wrapper로 실행 JAR를 만드는 빌드 스테이지와 Java 17 런타임만 포함하는 실행 스테이지를 분리한 멀티스테이지 Dockerfile을 작성한다.
- `predictor`에 `requirements.txt`의 런타임 의존성을 준비하는 빌드 스테이지와 애플리케이션 실행에 필요한 파일만 포함하는 실행 스테이지를 분리한 멀티스테이지 Dockerfile을 작성한다.
- 두 이미지의 최종 스테이지에 전용 비-root 사용자를 만들고 해당 사용자로 애플리케이션을 실행한다.
- `ticketing-api`의 `/actuator/health`와 `predictor`의 `/health`를 각각 호출하는 Docker `HEALTHCHECK`를 추가한다.
- 각 애플리케이션의 빌드 컨텍스트에서 로컬 캐시, 가상환경, 테스트·빌드 산출물, IDE 및 VCS 메타데이터 등 이미지 빌드에 불필요한 파일을 제외하는 `.dockerignore`를 작성한다.
- `ticketing-api` 이미지를 `ticketing-api` 디렉터리를 빌드 컨텍스트로 사용해 실제 빌드한다.
- `predictor` 이미지를 `predictor` 디렉터리를 빌드 컨텍스트로 사용해 실제 빌드한다.
- 두 컨테이너를 서로 독립적으로 실행하고 호스트에 게시한 포트를 통해 기존 health endpoint의 상태 코드와 응답을 검증한다.
- Docker가 보고하는 각 컨테이너의 health 상태가 `healthy`인지 검증한다.
- 이미지 설정의 실행 사용자와 실행 중 컨테이너의 UID를 확인해 root 사용자가 아님을 검증한다.
- 기존 Spring 및 Python 검증 책임을 변경하지 않고 저장소 루트의 `./scripts/verify.sh`를 다시 실행한다.
- 실제 빌드·실행 방법, 설계 판단, 검증 결과 및 남은 위험을 task 완료 결과에 기록한다.

## 제외 범위

- Docker Compose
- PostgreSQL 및 데이터베이스 연결
- 두 컨테이너 사이의 네트워크 연결 및 애플리케이션 간 통신
- GHCR을 포함한 이미지 레지스트리 게시
- 기존 GitHub Actions workflow 수정 및 컨테이너 빌드 CI 추가
- 기존 `scripts/verify.sh`의 책임 또는 내용 변경
- Kubernetes 및 kind 구성
- 티켓팅 도메인 로직
- ML 모델 학습 및 추론
- 로그인, 결제, 프론트엔드
- Python 의존성 lock file 또는 새로운 의존성 관리 도구 도입
- Docker Compose 등 새로운 컨테이너 오케스트레이션 도구 도입

## Acceptance Criteria

- [x] `ticketing-api/Dockerfile`이 빌드 스테이지와 실행 스테이지를 분리한 멀티스테이지 이미지로 작성되었다.
- [x] `ticketing-api` 빌드 스테이지가 저장소의 Gradle wrapper와 Java 17 설정을 사용해 실행 가능한 Spring Boot JAR를 생성한다.
- [x] `ticketing-api` 최종 이미지가 빌드 도구 없이 Java 17 런타임과 실행 JAR를 포함한다.
- [x] `predictor/Dockerfile`이 의존성 빌드 스테이지와 실행 스테이지를 분리한 멀티스테이지 이미지로 작성되었다.
- [x] `predictor` 이미지가 기존 pip `requirements.txt` 방식으로 런타임 의존성을 설치하고 `app.main:app`을 Uvicorn으로 실행한다.
- [x] 두 이미지의 최종 스테이지에 root가 아닌 실행 사용자가 명시되어 있다.
- [x] 실행 중인 두 컨테이너에서 확인한 UID가 `0`이 아니다.
- [x] `ticketing-api` 이미지의 Docker `HEALTHCHECK`가 컨테이너 내부의 `http://localhost:8080/actuator/health`를 검사한다.
- [x] `predictor` 이미지의 Docker `HEALTHCHECK`가 컨테이너 내부의 `http://localhost:8000/health`를 검사한다.
- [x] 두 컨테이너의 Docker health 상태가 제한 시간 안에 `healthy`가 된다.
- [x] `ticketing-api/.dockerignore`가 Gradle 캐시·빌드 산출물과 기타 불필요한 빌드 컨텍스트 파일을 제외한다.
- [x] `predictor/.dockerignore`가 가상환경·Python 캐시·테스트 캐시와 기타 불필요한 빌드 컨텍스트 파일을 제외한다.
- [x] `ticketing-api` 이미지가 실제로 빌드되고 컨테이너가 독립적으로 실행된다.
- [x] `predictor` 이미지가 실제로 빌드되고 컨테이너가 독립적으로 실행된다.
- [x] 호스트에서 게시 포트로 `ticketing-api`의 `/actuator/health`를 호출했을 때 HTTP 200과 `UP` 상태를 확인한다.
- [x] 호스트에서 게시 포트로 `predictor`의 `/health`를 호출했을 때 HTTP 200과 `{"status":"UP"}` 응답을 확인한다.
- [x] 기존 `.github/workflows/ci.yml`과 `scripts/verify.sh`를 변경하지 않았다.
- [x] Docker Compose, PostgreSQL, 컨테이너 간 통신, GHCR, GitHub Actions 수정, Kubernetes, 도메인 로직 및 ML 모델을 추가하지 않았다.
- [x] 저장소 루트의 `./scripts/verify.sh`가 통과한다.
- [x] 실제 변경 파일, 주요 설계 판단, 실행한 검증 명령과 결과, 남은 위험이 문서화되었다.

## 검증 명령

이미지 이름과 컨테이너 이름은 로컬 검증용으로만 사용한다. 포트 충돌을 피하기 위해 호스트 포트는 컨테이너 기본 포트와 다르게 게시할 수 있으며, 실제 사용한 값을 완료 결과에 기록한다.

```bash
docker build --tag flashscale-ticketing-api:day-05 ./ticketing-api
docker build --tag flashscale-predictor:day-05 ./predictor

docker run --detach --name flashscale-ticketing-api-day-05 \
  --publish 18080:8080 flashscale-ticketing-api:day-05
docker run --detach --name flashscale-predictor-day-05 \
  --publish 18000:8000 flashscale-predictor:day-05

curl --fail http://localhost:18080/actuator/health
curl --fail http://localhost:18000/health

docker inspect --format '{{.State.Health.Status}}' \
  flashscale-ticketing-api-day-05
docker inspect --format '{{.State.Health.Status}}' \
  flashscale-predictor-day-05

docker image inspect --format '{{.Config.User}}' \
  flashscale-ticketing-api:day-05
docker image inspect --format '{{.Config.User}}' \
  flashscale-predictor:day-05
docker exec flashscale-ticketing-api-day-05 id -u
docker exec flashscale-predictor-day-05 id -u

./scripts/verify.sh
```

검증이 끝나면 이번 task에서 생성한 두 컨테이너를 중지하고 제거한다. 이미지 제거는 필수 완료 조건에 포함하지 않는다.

## 예상 변경 파일

- `docs/tasks/day-05-containerization.md`
- `docs/retrospectives/day-05.md`
- `ticketing-api/Dockerfile`
- `ticketing-api/.dockerignore`
- `predictor/Dockerfile`
- `predictor/.dockerignore`

## 위험 요소

- 최초 이미지 빌드는 Java 및 Python 기반 이미지, Gradle 배포본, Maven 의존성, pip 패키지를 내려받기 위한 네트워크 접근이 필요하다.
- Python 런타임 의존성 버전이 고정되어 있지 않아 이미지 빌드 시점에 따라 설치 결과가 달라질 수 있다. 의존성 고정 정책은 이번 범위에서 변경하지 않는다.
- 멀티스테이지 빌드는 빌드 도구를 최종 이미지에서 제외하지만 기반 이미지와 애플리케이션 의존성 자체의 크기 및 보안 취약점을 제거하지는 않는다.
- Docker `HEALTHCHECK`에 사용할 HTTP 클라이언트가 최종 이미지에 없으면 상태 검사가 실행되지 않는다. 최종 이미지에서 실제 사용 가능한 최소 수단을 선택하고 컨테이너 실행으로 확인한다.
- Spring Boot는 Predictor보다 시작 시간이 길 수 있으므로 health check의 시작 유예와 재시도 설정이 너무 짧으면 정상 컨테이너가 일시적으로 `unhealthy`가 될 수 있다.
- 호스트의 `18080` 또는 `18000` 포트가 이미 사용 중이면 다른 호스트 포트로 게시하고 실제 검증 값을 기록한다.
- Docker daemon을 사용할 수 없거나 현재 사용자에게 Docker 실행 권한이 없으면 실제 이미지·컨테이너 검증이 제한될 수 있으며, 이 경우 원인과 수행하지 못한 검증을 명시한다.

## 완료 결과

- 실제 변경 파일은 `docs/tasks/day-05-containerization.md`, `docs/retrospectives/day-05.md`, `ticketing-api/Dockerfile`, `ticketing-api/.dockerignore`, `predictor/Dockerfile`, `predictor/.dockerignore`의 6개다. 기존 `.github/workflows/ci.yml`과 `scripts/verify.sh`는 변경하지 않았다.
- `ticketing-api`는 `eclipse-temurin:17-jdk-jammy` 빌드 스테이지에서 저장소의 Gradle wrapper 9.5.1로 `bootJar`를 실행하고, `eclipse-temurin:17-jre-jammy` 실행 스테이지에는 생성된 JAR만 복사했다. 최초 검토한 Alpine 태그는 현재 ARM64 manifest가 없어 실제 빌드에 실패했으므로 Java 17을 유지하면서 ARM64와 AMD64를 지원하는 Jammy 태그로 변경했다.
- `ticketing-api` 최종 이미지에는 health check가 기반 이미지의 암묵적인 도구 구성에 의존하지 않도록 `curl`을 명시적으로 설치했다. 시스템 사용자와 그룹 `app`을 만들고 `USER app`을 지정한 뒤 `curl`로 `/actuator/health`를 검사한다.
- `predictor`는 `python:3.10-slim` 빌드 스테이지에서 기존 `requirements.txt`를 `/install` prefix에 설치하고, 실행 스테이지에 해당 런타임 의존성과 `app` 패키지만 복사했다. 개발용 reload 없이 `python -m uvicorn app.main:app --host 0.0.0.0 --port 8000`으로 실행한다.
- `predictor` 최종 이미지에도 시스템 사용자와 그룹 `app`을 만들고 `USER app`을 지정했다. Python 표준 라이브러리 `urllib.request`로 `/health`를 검사해 별도 HTTP 클라이언트 의존성을 추가하지 않았다.
- 두 `.dockerignore`는 각 빌드에 필요 없는 로컬 Gradle 캐시와 빌드 결과, Python 가상환경과 캐시, 테스트 파일, IDE 및 VCS 메타데이터를 빌드 컨텍스트에서 제외한다.
- `docker build --tag flashscale-ticketing-api:day-05 ./ticketing-api`와 `docker build --tag flashscale-predictor:day-05 ./predictor`가 성공했다. 최종 로컬 이미지 크기는 각각 111,878,714 bytes와 49,651,487 bytes였다.
- `18080:8080`과 `18000:8000`으로 두 컨테이너를 독립 실행했다. 호스트에서 Spring health endpoint는 `{"groups":["liveness","readiness"],"status":"UP"}`, Predictor health endpoint는 `{"status":"UP"}`를 반환했다.
- Docker가 보고한 두 컨테이너의 health 상태는 모두 `healthy`였다. 이미지 설정의 실행 사용자는 모두 `app`이었고, 실행 중 `id -u` 결과는 모두 `999`로 root UID `0`이 아님을 확인했다.
- 저장소 루트에서 `./scripts/verify.sh`를 실행했다. 최초 샌드박스 실행은 사용자 Gradle cache lock 파일 접근 제한으로 실패했지만, 동일 명령을 승인된 환경에서 재실행해 Spring의 Spotless·Checkstyle·테스트와 Python의 Ruff 포맷·lint·pytest가 모두 통과했다. Python 테스트는 1개가 통과했다.
- 검증용 컨테이너 두 개는 검증 후 중지하고 제거했으며, 재사용 가능한 로컬 이미지는 남겼다.
- Docker Compose, PostgreSQL, 컨테이너 간 통신, GHCR, GitHub Actions 수정, Kubernetes, 도메인 로직 및 ML 모델은 추가하지 않았다.
- 남은 위험은 Python 의존성과 기반 이미지 태그가 digest로 고정되지 않아 향후 빌드 결과가 달라질 수 있다는 점, 현재 검증이 로컬 ARM64 Docker daemon에서 수행되어 AMD64 빌드는 직접 실행하지 않았다는 점, 이미지 취약점 스캔과 레지스트리 배포는 이번 범위에 포함하지 않았다는 점이다.
