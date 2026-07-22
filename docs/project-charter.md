# 문제 정의
트래픽 급증이 예상되는 티켓팅 서비스에서 반응형 오토스케일링은 부하가 발생한 뒤에야 대응한다. FlashScale은 과거 트래픽과 현재 지표를 사용해 미래 부하를 예측하고, 선제적으로 확장했을 때 얻는 효과를 실험한다.

# 핵심 실험
동일한 부하 시나리오에서 다음 세 전략을 비교한다.

1. 고정 replica
2. CPU 기반 HPA
3. 트래픽 예측 기반 scaling

# 핵심 측정값
- 요청 성공률
- p95/p99 latency
- 처리량
- 품절된 좌석의 중복 예약 발생 여부
- replica 수와 리소스 사용량
- 스케일 아웃 시작 시점
- 예측값과 실제 요청량의 오차

# v1 포함 범위
- Spring Boot 티켓팅 API
- PostgreSQL
- 동시 예약 방지
- k6 부하 생성
- Prometheus/Grafana 관측
- 트래픽 데이터 수집
- baseline 및 Gradient Boosting 예측 모델
- 고정/HPA/예측 기반 scaling 비교
- Docker Compose, GitHub Actions, GHCR, kind

# v1 제외 범위
- 로그인과 사용자 관리
- 결제
- 프론트엔드
- 실제 좌석 선택 UI
- Redis 및 분산 대기열
- Kafka, Airflow, Kubeflow, KServe, Feast
- 딥러닝 시계열 모델
- 자동 재학습
- 상시 클라우드 배포

# 30일 완료 기준
동일한 부하 시나리오를 세 가지 scaling 전략으로 재현하고, 예측 기반 scaling이 지연시간·성공률·리소스 비용에 미친 영향을 수치와 그래프로 설명할 수 있다.