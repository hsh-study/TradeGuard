# TradeGuard MVP Roadmap

## 1. 목표

MVP의 목표는 실계좌 자동매매가 아니라 다음 흐름을 재현 가능하고 안전하게 검증하는 것이다.

```text
관심종목
  -> 일봉 수집/저장
  -> 기술지표 계산/저장
  -> 종가베팅 후보 점수화
  -> 리스크 판정
  -> 모의 주문
  -> 신호/주문 이력 조회와 알림
```

## 2. 완료 기준

MVP 1차는 다음 조건을 모두 만족할 때 완료로 본다.

- 외부에서 관심종목을 등록하고 조회할 수 있다.
- 종목별 일봉을 저장하고 중복 없이 조회할 수 있다.
- 저장된 데이터로 지표 스냅샷을 계산하고 저장할 수 있다.
- 전략 실행 결과와 점수 근거가 이력으로 남는다.
- 리스크 승인된 주문만 Fake Broker에 전달된다.
- 중복 주문이 애플리케이션과 DB 양쪽에서 차단된다.
- 거절 신호를 포함한 모든 판정 이력을 조회할 수 있다.
- 핵심 도메인과 주요 application flow가 자동 테스트로 보호된다.
- 실제 주문 API를 호출하는 실행 경로가 없다.
- 로컬 실행, MySQL 실행, API 사용법이 문서화되어 있다.

## 3. 현재 상태

상태 표기:

- `완료`: 코드와 기본 테스트가 존재함
- `부분 완료`: 기반 구조는 있으나 유스케이스 연결 또는 검증이 부족함
- `미구현`: 구현이 시작되지 않았거나 빈 스켈레톤만 존재함

| 영역 | 상태 | 현재 내용 |
| --- | --- | --- |
| 관심종목 등록/조회 | 부분 완료 | Port와 persistence adapter 경계 적용. validation, 중복 응답 정책이 필요 |
| 일봉 모델/저장 구조 | 부분 완료 | KIS 읽기 전용 수집과 저장/기간 조회 존재. Web API와 100건 초과 분할 조회 없음 |
| 기술지표 | 완료 | MA, RSI, MACD, Bollinger Band와 단위 테스트 존재 |
| 지표 저장 | 부분 완료 | 계산·저장 orchestration과 종목·거래일 upsert 존재. 조회 API 없음 |
| 종가베팅 전략 | 완료 | 분석 유스케이스에 연결됐으며 점수 계산과 테스트 존재 |
| 신호 저장 | 부분 완료 | 논리 키 upsert와 상태 갱신 존재. 조회 API와 동시성 검증 필요 |
| RiskManager | 부분 완료 | 기본 정책과 단위 테스트 존재. 경계/복수 위반/동시성 테스트 필요 |
| 모의 주문 | 부분 완료 | OrderService와 FakeBrokerAdapter 존재. 외부 API와 통합 테스트 없음 |
| 중복 주문 방지 | 부분 완료 | 사전 조회 존재. DB unique constraint 없음 |
| 알림 | 미구현 | 빈 adapter만 존재 |
| KIS 연동 | 부분 완료 | 모의투자 OAuth와 일봉 조회 구현. 계좌/주문 연동은 의도적으로 제외 |
| 운영 관측성 | 미구현 | 구조화 로그, metric, health 세분화 없음 |

## 4. 구현 단계

### Phase 1. 아키텍처 경계 정리

목표: application 계층에서 JPA 타입 의존을 제거한다.

- `StockPort` 출력 포트와 persistence adapter 추가
- 관심종목 조회 inbound port 추가
- Controller 응답을 `StockEntity`가 아닌 DTO/domain projection으로 변경
- 일봉 저장/조회 port 정의
- 지표 저장/조회 port 정의
- 알림 port 정의 후 `NotificationAdapter` 연결

완료 조건:

- `application` 패키지가 `adapter` 패키지를 import하지 않는다.
- ArchUnit 또는 패키지 의존성 테스트가 규칙을 검증한다.

### Phase 2. 시장 데이터 파이프라인

목표: 재현 가능한 분석 입력을 만든다.

- 일봉 upsert 유스케이스 구현
- 종목/기간별 일봉 조회 구현
- `(stock_code, trade_date)` 중복 정책 검증
- CSV fixture 또는 Fake Market Data Adapter 제공
- 데이터 정렬, 결측 거래일, 잘못된 OHLCV validation 추가
- 향후 KIS 시세 조회를 위한 `MarketDataPort` 정의

완료 조건:

- API 또는 테스트 fixture로 60개 이상의 일봉을 적재할 수 있다.
- 같은 일봉을 재적재해도 중복 행이 생성되지 않는다.
- 외부 API 없이 전체 분석 테스트를 재현할 수 있다.

### Phase 3. 지표와 전략 실행 유스케이스

목표: 저장된 일봉에서 후보 신호를 생성한다.

- 지표 계산 application service 구현
- `IndicatorSnapshot`과 Entity 양방향 매핑 구현
- 종목/거래일 기준 지표 중복 방지
- 전략 실행 inbound port와 service 구현
- 활성 관심종목 전체를 평가하는 수동 실행 endpoint 또는 scheduler 구현
- 데이터 부족 시 명확한 skip 결과 저장

완료 조건:

- 특정 종목과 날짜를 입력해 동일한 지표와 신호를 반복 생성할 수 있다.
- 점수와 reason이 DB에 저장되고 조회된다.
- 전략은 broker port를 직접 호출하지 않는다.

### Phase 4. 안전한 모의 주문 흐름

목표: 리스크 승인부터 모의 주문 저장까지 일관되게 처리한다.

- 주문 요청 REST API 또는 전략 실행 orchestration 연결
- 리스크 승인/거절 결과 DTO 정의
- 거절 시 예외 대신 판정 결과를 반환하고 이력 보존
- 신호 Entity 식별자를 유지해 상태를 update하도록 변경
- 주문 중복 복합 unique constraint 추가
- DB 중복 예외를 도메인 거절 결과로 변환
- Broker 실패 시 주문 상태와 재시도 가능 여부 기록
- 주문 application integration test 추가

완료 조건:

- 승인 요청은 신호와 주문 이력이 각각 한 건으로 남는다.
- 거절 요청은 broker를 호출하지 않고 거절 사유가 남는다.
- 동시에 같은 주문을 요청해도 한 건만 접수된다.

### Phase 5. 조회, 알림, 운영 준비

목표: 모의 실행 결과를 사람이 검증할 수 있게 한다.

- 거래일/종목/상태별 신호 조회 API
- 주문 요청과 상태 조회 API
- 고득점 후보 및 리스크 거절 알림
- actuator health와 DB 상태 확인
- 비밀정보 마스킹과 구조화 로그
- Flyway 또는 Liquibase migration 도입
- MySQL Testcontainers 통합 테스트
- API 예외 응답과 validation 규격 통일

완료 조건:

- 한 거래일의 분석, 판정, 주문 결과를 API와 로그로 추적할 수 있다.
- 새 DB에 migration을 적용해 애플리케이션을 시작할 수 있다.
- CI에서 unit/integration test가 통과한다.

## 5. MVP 이후

다음 항목은 MVP 1차 범위에 포함하지 않는다.

- 실계좌 주문
- 시장가 주문
- 자동 매도와 포지션 청산
- 실시간 체결/호가 스트리밍
- 다중 전략 포트폴리오 최적화
- 백테스트 엔진
- 운영자용 Web UI
- MSA, Kafka, Kubernetes

우선순위는 “실주문 연결”보다 데이터 품질, 전략 재현성, 리스크 이력, 모의 주문 안정성에 둔다.

## 6. 테스트 계획

| 계층 | 테스트 범위 |
| --- | --- |
| Domain | 지표 수식, 전략 점수 경계, 리스크 정책, 상태 전이 |
| Application | 승인/거절 흐름, broker 호출 여부, 이력 저장, 실패 처리 |
| Persistence | 매핑, unique constraint, 기간 조회, enum 저장 |
| Web | validation, 상태 코드, 오류 응답, JSON 계약 |
| Integration | H2 빠른 테스트와 MySQL Testcontainers 호환성 |

매 단계는 `./gradlew test` 통과와 관련 문서 업데이트를 완료 조건으로 한다.

## 7. 다음 구현 순서

가장 가까운 작업 순서는 다음과 같다.

1. KIS 일봉 100건 초과 구간 분할 조회 구현
2. 분석 실행 REST API와 활성 관심종목 실행 orchestration 구현
3. 리스크 거절 이력 보존
4. 주문 중복 unique constraint와 통합 테스트 추가
5. 모의 주문 및 이력 조회 REST API 추가
