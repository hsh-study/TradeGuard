# Risk Policy

MVP 1차 RiskManager 정책입니다.

## 주문 제한

- 동일 날짜, 동일 종목, 동일 전략, 동일 매수 방향의 중복 주문을 금지한다.
- 1회 주문금액은 기본 100,000원을 초과할 수 없다.
- 점수 70점 미만의 신호는 주문 요청을 만들 수 없다.
- 주문 수량은 1주 이상이어야 한다.
- 지정가 주문만 허용한다.
- 실계좌 주문은 지원하지 않는다.

## 실거래 차단

`tradeguard.real-trading-enabled`나 유사 환경변수가 true여도 MVP에서는 실계좌 주문을 실행하지 않는다. `KisBrokerAdapter`는 실제 API 호출을 구현하지 않는다.

## 거절 사유 코드

- `SCORE_BELOW_70`
- `ONLY_BUY_CANDIDATE_SUPPORTED_IN_MVP`
- `ONLY_LIMIT_ORDER_ALLOWED`
- `QUANTITY_LESS_THAN_ONE`
- `ORDER_AMOUNT_EXCEEDS_LIMIT`
- `DUPLICATE_ORDER`
