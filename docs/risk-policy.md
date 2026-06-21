# TradeGuard Risk Policy

## 뉴스 사용 제한

- 뉴스는 operator-facing research evidence와 수동 확인 대상으로만 사용한다.
- 뉴스 또는 sentiment만으로 자동매수·자동매도, 수량·지정가 결정, readiness/kill switch 변경을 금지한다.
- 동일 사건에 공시 evidence가 있으면 공시를 우선한다.
- HIGH/RISK 뉴스도 thesis를 자동으로 `BROKEN` 처리하지 않고 Morning Note에 수동 검토 항목으로 남긴다.

## 1. 목적

RiskManager는 전략 신호를 주문으로 전환하기 전에 손실 가능성과 잘못된 주문을 제한하는 최종 도메인 정책이다. 전략 점수가 높더라도 리스크 검증을 우회할 수 없다.

전략 기반 `OrderService` 정책은 모의 주문용이고, 별도의 live trading 정책은 운영자 수동 주문을 보호한다. 어느 경로도 수익을 보장하거나 투자 판단을 대신하지 않는다.

## 2. 기본 원칙

- 전략은 `TradingSignal`만 생성한다.
- `RiskManager`가 승인한 신호만 `OrderService`가 브로커로 전달한다.
- 모든 주문은 지정가여야 한다.
- 전략 신호 기반 모의 주문은 매수 후보와 매수 주문만 허용한다.
- live UI는 선택 계좌의 수동 지정가 매수·매도를 지원한다.
- 실계좌 주문은 REAL 계좌 선택과 `realTradingConfirmed=true`가 있는 수동 요청만 허용한다.
- 전략, scheduler, alert 및 리서치 결과는 자동매수 주문을 생성하지 않는다.
- 기존 `LIVE_POSITION_EXIT_MONITOR`는 두 live flag가 켜진 장중에 저장된 OPEN 포지션의 익절·손절 규칙에 따른 자동 지정가 매도만 수행할 수 있다.
- Earnings Analysis는 후보 reason과 점수 보강만 수행하며 자동 주문을 실행하지 않는다.
- 한 번의 판정에서 발견된 모든 거절 사유를 반환한다.

## 3. 승인 조건

다음 조건을 모두 충족해야 승인된다.

| 정책 | 승인 조건 | 거절 코드 |
| --- | --- | --- |
| 최소 점수 | 신호 점수 `>= 70` | `SCORE_BELOW_70` |
| 지원 신호/방향 | `BUY_CANDIDATE`와 `BUY` 조합 | `ONLY_BUY_CANDIDATE_SUPPORTED_IN_MVP` |
| 주문 유형 | `LIMIT` | `ONLY_LIMIT_ORDER_ALLOWED` |
| 최소 수량 | 수량 `>= 1` | `QUANTITY_LESS_THAN_ONE` |
| 1회 주문금액 | `수량 x 지정가 <= 100,000원` | `ORDER_AMOUNT_EXCEEDS_LIMIT` |
| 중복 주문 | 동일 주문이 존재하지 않음 | `DUPLICATE_ORDER` |

기본 최대 주문금액은 100,000원이다. `RiskManager(BigDecimal maxOrderAmount)` 생성자로 테스트나 향후 설정에서 값을 바꿀 수 있지만, 현재 Spring 설정은 기본값을 사용한다.

## 4. 중복 주문 정의

다음 값이 모두 같은 주문 요청은 중복으로 판단한다.

- 거래일
- 종목 코드
- 전략명
- 주문 방향

현재 MVP는 매수만 허용하므로 사실상 “동일 날짜, 동일 종목, 동일 전략의 매수 요청은 한 번만 허용”한다.

사전 조회만으로는 동시에 들어온 요청의 race condition을 완전히 차단할 수 없으므로 아래 복합 unique constraint를 적용한다.

```text
(trade_date, stock_code, strategy_name, side)
```

`OrderService`는 리스크 승인 후 주문을 `CREATED` 상태로 DB에 먼저 예약한다. unique constraint 위반은 `DUPLICATE_ORDER` 거절 결과로 변환하며, 예약에 성공한 요청만 broker에 전달한다.

## 5. 점수 정책

`ClosingBetStrategy`의 현재 점수 기준은 다음과 같다.

| 조건 | 점수 | reason |
| --- | ---: | --- |
| MA5 > MA20 | +15 | `MA5_ABOVE_MA20` |
| 종가 > MA20 | +10 | `CLOSE_ABOVE_MA20` |
| 당일 거래량 >= 최근 20일 평균의 200% | +20 | `VOLUME_SPIKE_20D_200PCT` |
| 종가 위치가 당일 저가-고가 범위의 80% 이상 | +15 | `CLOSE_NEAR_HIGH` |
| `(고가 - 종가) / 종가 >= 5%` | -15 | `LONG_UPPER_TAIL` |
| 전일 종가 대비 상승률 >= 15% | -10 | `SHARP_RISE_FROM_PREVIOUS_CLOSE` |
| 거래대금 >= 500억원 | +15 | `TRADING_VALUE_OVER_50B_KRW` |

점수 70은 주문 생성의 필요조건일 뿐 충분한 투자 근거가 아니다. 점수 기준을 변경할 때는 과거 데이터 검증 결과, 변경 이유, 적용일을 함께 기록해야 한다.

## 6. 판정과 상태 전이

승인 시:

```text
TradingSignal: CREATED -> RISK_APPROVED -> ORDER_REQUESTED
OrderRequest:  CREATED -> REQUESTED -> ACCEPTED
```

Broker 호출 실패 시:

```text
TradingSignal: CREATED -> RISK_APPROVED
OrderRequest:  CREATED -> BROKER_FAILED
```

Broker 예외는 주문 예약을 제거하거나 `CREATED` 상태로 방치하지 않는다. 주문에 `failureReason`, `failedAt`, `retryable`을 기록하고 `brokerOrderNo`는 null로 유지한다. 현재 RuntimeException 기반 Broker 실패는 재시도 가능 후보로 기록하지만 자동 재시도는 수행하지 않는다. 실제 Broker 요청 성공 전이므로 신호는 `ORDER_REQUESTED`로 변경하지 않고 `RISK_APPROVED`를 유지한다.

### 수동 재시도

```text
BROKER_FAILED(retryable=true)
  -> RETRY_REQUESTED
  -> REQUESTED
  -> ACCEPTED

BROKER_FAILED(retryable=true)
  -> RETRY_REQUESTED
  -> BROKER_FAILED
```

- `BROKER_FAILED`와 `retryable=true`를 모두 만족하는 주문만 재시도할 수 있다.
- 재시도는 기존 `order_requests` row를 갱신하며 새 row를 만들지 않는다.
- `BROKER_FAILED -> RETRY_REQUESTED`는 DB 조건부 update로 선점한다.
- 동시에 같은 orderId를 재시도하면 한 요청만 선점하고 Broker를 호출한다.
- 재시도 실패 시 실패 사유와 시각을 갱신하고 `BROKER_FAILED`로 돌아간다.
- `ACCEPTED`, `REJECTED`, `CANCELED`, `FILLED`, `PARTIALLY_FILLED`는 재시도할 수 없다.
- 자동 재시도와 backoff는 구현하지 않는다.

### 정체된 재시도 복구

`BROKER_FAILED -> RETRY_REQUESTED` 선점 시 `retry_requested_at`을 함께 기록한다. `RETRY_REQUESTED`가 설정된 기준 시간보다 오래 지속되면 stuck retry로 판단한다. 기본 기준은 5분이며 `tradeguard.order.retry-stuck-threshold-minutes`로 변경할 수 있다.

```text
RETRY_REQUESTED(stuck)
  -> BROKER_FAILED(retryable=true)
```

- 운영 조회는 `status=RETRY_REQUESTED`이고 `retry_requested_at <= 기준시각 - threshold`인 주문만 반환한다.
- 수동 복구는 동일한 threshold를 넘긴 `RETRY_REQUESTED` 주문에만 허용한다.
- 복구 시 `failureReason`은 `Retry request stuck recovered: {reason}`으로 기록하고 `failedAt`을 복구 시각으로 갱신한다.
- 복구 후 `retryable=true`를 유지하므로 운영자가 원인을 확인한 뒤 다시 수동 재시도할 수 있다.
- `ACCEPTED` 또는 `BROKER_FAILED`로 전이가 끝나면 `retry_requested_at`은 null로 비운다.
- 자동 재시도와 자동 stuck recovery scheduler는 구현하지 않는다.

`order_requests.signal_id`는 주문을 생성한 TradingSignal을 nullable FK로 참조한다. signalId 기반 주문은 path variable의 ID를 저장하고, 논리 키 기반 주문은 조회한 신호의 JPA ID를 별도로 조회해 저장한다. 도메인 `TradingSignal`에는 persistence ID를 추가하지 않는다.

수동 재시도 성공 시 `signal_id`가 있으면 연결된 TradingSignal을 `ORDER_REQUESTED`로 변경한다. 재시도 실패 시 신호는 변경하지 않는다. V3 이전 주문처럼 `signal_id`가 null이면 신호 동기화를 건너뛰되 주문 재시도는 실패시키지 않는다.

### 상태 변경 감사 이력

`trading_signal_status_histories`와 `order_request_status_histories`는 주요 상태 전이의 이전 상태, 이후 상태, 이유, 발생 시각을 대상 ID와 함께 저장한다. 도메인 모델에는 persistence ID나 JPA 관계를 추가하지 않고 application service가 outbound history port를 호출한다.

감사 대상 전이는 다음과 같다.

- TradingSignal `CREATED -> RISK_APPROVED`: 리스크 정책 승인
- TradingSignal `RISK_APPROVED -> ORDER_REQUESTED`: 최초 주문 또는 수동 재시도 Broker 승인
- OrderRequest `CREATED -> ACCEPTED`: 최초 모의 주문 Broker 승인
- OrderRequest `CREATED -> BROKER_FAILED`: 최초 Broker 호출 실패
- OrderRequest `BROKER_FAILED -> RETRY_REQUESTED`: 수동 재시도 선점
- OrderRequest `RETRY_REQUESTED -> ACCEPTED`: 수동 재시도 성공
- OrderRequest `RETRY_REQUESTED -> BROKER_FAILED`: 수동 재시도 실패
- OrderRequest `RETRY_REQUESTED -> BROKER_FAILED`: stuck retry 수동 복구

조회 결과는 `created_at`, `id` 오름차순으로 반환한다. `reason`에는 정책 승인, 수동 재시도 요청, Broker 실패 사유 또는 stuck 복구 사유를 기록하며 API Key, App Secret, 계좌번호 같은 비밀정보는 기록하지 않는다.

거절 시:

```text
TradingSignal: CREATED -> RISK_REJECTED
OrderRequest:  broker에 전달하지 않음
```

`OrderService`는 거절을 예외로 처리하지 않고 `MockOrderResult`로 반환한다. 신호의 `RISK_REJECTED` 상태와 모든 `riskReasons`를 저장하며, broker 호출과 주문 요청 저장은 수행하지 않는다.

## 7. 입력 단계 방어

`OrderRequest` 생성자는 아래 잘못된 요청을 도메인 객체 생성 단계에서 차단한다.

- 빈 종목 코드
- 1주 미만 수량
- 0 이하 또는 null 지정가
- `LIMIT` 이외 주문 유형
- null 주문 방향, 전략명, 거래일

따라서 일부 잘못된 입력은 `RiskDecision`이 만들어지기 전에 `IllegalArgumentException` 또는 `NullPointerException`으로 실패한다. Web API 추가 시 Bean Validation과 일관된 오류 응답을 앞단에 적용해야 한다.

## 8. 실거래 제한 정책

- 전략 기반 모의 주문은 `FakeBrokerAdapter`를 유지한다.
- 수동 live 주문은 두 feature flag가 활성화된 경우 `KisLiveTradingOrderAdapter`를 사용한다.
- REAL 주문은 계좌 선택, 확인값, readiness, kill switch, 거래시간, 금액 한도와 LIMIT 조건을 모두 검사한다.
- 시장가, 자동매수, 공매도, 신용, 미수 주문과 확인 없는 실전 주문은 허용하지 않는다.
- 계좌번호와 KIS/DART secret은 DB 암호문으로 저장하고 API/UI에는 마스킹 값만 반환한다.
- 계좌 선택 주문은 로컬 단일 운영자용으로 직렬화한다. 다중 사용자 환경은 지원하지 않는다.

## 9. 향후 리스크 정책

현재 MVP 이후 검토할 정책은 다음과 같다.

- 일일 총 주문금액과 주문 횟수 제한
- 종목별/시장별 최대 노출
- 보유 현금 및 기존 포지션 확인
- 가격 제한폭과 호가 단위 검증
- 데이터 최신성 및 장 운영일 검증
- 연속 손실, 최대 손실, drawdown 기반 중지
- 거래 정지, 관리종목 등 종목 상태 필터
- 주문 만료, 취소, 부분 체결 처리
- 시스템 전역 kill switch

각 정책은 기본 거절(fail closed)을 원칙으로 하고 독립된 테스트를 가져야 한다.

## 10. 테스트 기준

RiskManager 변경 시 최소한 다음 경계를 검증한다.

- 점수 69 거절, 70 승인
- 주문금액 100,000원 승인, 100,001원 거절
- 중복 주문 거절
- 매도 또는 매도 후보 거절
- 지정가 이외 주문 거절
- 복수 위반 시 모든 사유 반환
- 승인/거절에 따른 신호 상태
- Broker 실패 시 `BROKER_FAILED` 상태와 실패 metadata 저장
- Broker 실패 시 신호가 `RISK_APPROVED`를 유지하고 `brokerOrderNo`가 null인지 확인
- 수동 재시도가 같은 row를 사용하고 Broker를 한 번만 호출하는지 확인
- 재시도 불가 상태 및 동시 선점 실패가 Broker를 호출하지 않는지 확인
- 동시 요청에서 DB unique constraint 동작
