# Task: Day 1 프로젝트 기반 구성

## 목적

FlashScale이 검증하려는 문제와 실험 범위를 문서화하고, 이후 작업을 일관된 규칙으로 수행할 수 있도록 저장소 구조와 에이전트 협업 문서 기반을 마련한다.

## 작업 범위

- 최상위 디렉터리 구조를 생성한다.
- 프로젝트 목표, 핵심 실험, 측정값과 30일 완료 기준을 프로젝트 차터에 기록한다.
- v1 포함 범위와 제외 범위를 명시한다.
- 루트 `AGENTS.md`에 작업 범위, 아키텍처, 검증 및 변경 정책을 기록한다.
- Task, ADR, 일일 회고 템플릿을 추가한다.
- 저장소 공통 불필요 파일을 `.gitignore`로 제외한다.
- 초기 변경사항을 Git에 기록한다.

## 제외 범위

- Spring Boot 및 FastAPI 애플리케이션 구현
- 티켓팅 비즈니스 로직과 데이터베이스 연결
- 예측 모델과 애플리케이션 간 통신
- Docker, Kubernetes, 모니터링 및 부하 테스트 구성
- 공통 검증 스크립트 구현

## Acceptance Criteria

- [x] 최상위 디렉터리 구조가 생성되었다.
- [x] 프로젝트 목표와 핵심 실험이 문서화되었다.
- [x] v1 포함/제외 범위가 명시되었다.
- [x] 루트에 `AGENTS.md`가 존재한다.
- [x] Task, ADR, 회고 템플릿이 존재한다.
- [x] Codex가 `AGENTS.md`와 task를 읽고 범위를 설명할 수 있다.
- [x] 초기 변경사항이 커밋되었다.

## 검증 명령

```bash
find . -maxdepth 2 -type d | sort
git status --short
git diff --cached --check
git check-ignore .DS_Store infra/.DS_Store
git ls-files | grep '\.DS_Store'
```

Day 1에는 `scripts/verify.sh`가 아직 없으므로 전체 검증 스크립트는 실행하지 않는다. 마지막 명령은 출력이 없고 종료 코드가 1이면 `.DS_Store`가 추적되지 않는 정상 상태다.

## 예상 변경 파일

- `.gitignore`
- `AGENTS.md`
- `docs/project-charter.md`
- `docs/tasks/TASK_TEMPLATE.md`
- `docs/tasks/day-01-bootstrap.md`
- `docs/adr/ADR_TEMPLATE.md`
- `docs/retrospectives/DAY_TEMPLATE.md`
- `docs/retrospectives/day-01.md`
- 최상위 애플리케이션·인프라 디렉터리의 자리표시자

## 위험 요소

- 초기 범위가 모호하면 이후 task가 핵심 실험과 무관한 기능으로 확대될 수 있다. 프로젝트 차터에 포함·제외 범위와 완료 기준을 함께 기록한다.
- `.gitignore` 규칙은 이미 추적 중인 파일을 자동으로 제거하지 않는다. 추적 여부를 별도로 확인한다.
- Day 1에는 실행 가능한 애플리케이션과 통합 검증 스크립트가 없으므로 문서와 저장소 구조를 중심으로 검증한다.

## 완료 결과

- FlashScale의 문제 정의, 세 가지 scaling 전략, 핵심 측정값과 v1 범위를 프로젝트 차터에 기록했다.
- 최상위 디렉터리와 `AGENTS.md`, Task·ADR·회고 템플릿을 준비했다.
- `.DS_Store`를 저장소 추적 대상에서 제외했다.
- 변경사항을 커밋하고 Day 1 acceptance criteria를 모두 확인했다.
