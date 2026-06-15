# Live Trading Operations

## Scope

TradeGuard의 live trading은 운영자가 승인한 현금 지정가 주문만 지원한다.
시장가, 자동매수, 공매도, 신용, 미수 주문은 지원하지 않는다.

운영 점검과 credential 교체 중에는 신규 주문을 실행하지 않는다.

## Pre-Deployment Checklist

1. 기본 feature flag가 모두 꺼져 있는지 확인한다.

   ```text
   LIVE_TRADING_ENABLED=false
   KIS_TRADING_ENABLED=false
   ```

2. 대상 환경을 확인한다.

   ```text
   KIS_TRADING_ENVIRONMENT=REAL|DEMO
   KIS_READ_ONLY_ENVIRONMENT=REAL|DEMO
   ```

   `REAL`은 실전 도메인, `DEMO`는 모의 도메인을 사용한다.
   두 환경의 access token cache는 서로 분리된다.

3. 다음 secret이 배포 환경의 secret store에 설정됐는지 확인한다.

   ```text
   KIS_APP_KEY
   KIS_APP_SECRET
   KIS_ACCOUNT_NUMBER
   KIS_ACCOUNT_PRODUCT_CODE
   ```

   원문 값을 로그, 이슈, 메신저 또는 readiness 응답에 기록하지 않는다.

4. DB kill switch를 활성화한 상태로 배포한다.

   ```sh
   curl -X POST 'http://localhost:8080/api/live-trading/kill-switch' \
     -H 'Content-Type: application/json' \
     -d '{"enabled":true,"reason":"PRE_DEPLOYMENT_CHECK"}'
   ```

5. calendar 동기화와 검증을 확인한다.

   ```sh
   curl -X POST \
     'http://localhost:8080/api/market-calendar/sync?year=2026'
   curl \
     'http://localhost:8080/api/market-calendar/validation?year=2026'
   ```

6. 다음 주문 정책 설정을 검토한다.

   ```text
   LIVE_MAX_ALLOWED_ORDER_AMOUNT
   LIVE_BUY_COMMISSION_RATE
   LIVE_SELL_COMMISSION_RATE
   LIVE_SELL_TAX_RATE
   LIVE_ORDER_AUTO_CANCEL_ENABLED
   LIVE_BUY_ORDER_EXPIRE_MINUTES
   LIVE_SELL_ORDER_EXPIRE_MINUTES
   LIVE_CANCEL_BEFORE_MARKET_CLOSE_MINUTES
   ```

7. token 상태와 종합 readiness를 확인한다.

   ```sh
   curl 'http://localhost:8080/api/kis/token/status'
   curl 'http://localhost:8080/api/live-trading/readiness'
   ```

8. 모의환경 smoke test를 먼저 수행한다. 실전환경에서는 자동 smoke
   test를 실행하지 않는다.

9. 모든 blocking reason을 해소한 뒤 마지막 단계에서만 kill switch를
   해제한다. Feature flag 변경과 kill switch 해제를 동시에 수행하지 않는다.

## Readiness Policy

다음 조건은 blocking이다.

- `LIVE_TRADING_ENABLED` 비활성
- `KIS_TRADING_ENABLED` 비활성
- 계좌번호 또는 계좌상품코드 미설정
- app key 또는 app secret 미설정
- live 환경 token 없음, 만료 또는 만료 임박
- DB kill switch 활성
- 현재 연도 DB calendar 없음
- 지정가 외 주문 유형
- 주문 한도, 세금 또는 수수료 설정 오류

현재 시장이 닫혀 있는 상태는 warning이다. 장외에도 배포 사전 점검은
가능해야 하지만, 실제 주문 API는 기존 장 운영시간 검사를 별도로 수행한다.

자동취소 비활성도 warning이다. 기본값은 의도적으로 `false`다.

## Credential Rotation

1. 신규 주문 전에 DB kill switch를 활성화한다.
2. 미체결 주문을 조회하고 필요하면 취소한다.
3. `LIVE_TRADING_ENABLED=false` 또는 배포 플랫폼의 traffic 차단 정책으로
   신규 요청을 중지한다.
4. secret store에서 `KIS_APP_KEY`, `KIS_APP_SECRET`을 교체한다.
5. 필요하면 계좌번호와 계좌상품코드도 같은 절차로 교체한다.
6. 애플리케이션을 재시작한다.

   현재 token cache는 MEMORY 방식이므로 재시작하면 REAL/DEMO cache가
   모두 무효화된다. 다중 인스턴스 환경에서는 각 인스턴스를 순차적으로
   재시작해야 하며 token cache는 공유되지 않는다.

7. 새 credential로 환경별 token을 발급한다.

   ```sh
   curl -X POST \
     'http://localhost:8080/api/kis/token/refresh?environment=DEMO'
   curl -X POST \
     'http://localhost:8080/api/kis/token/refresh?environment=REAL'
   ```

   실제로 사용하는 환경만 호출한다. 수동 refresh는 기존 cache를 새
   token으로 교체한다.

8. token status와 readiness를 확인한다.

   ```sh
   curl 'http://localhost:8080/api/kis/token/status'
   curl 'http://localhost:8080/api/live-trading/readiness'
   ```

9. 모의환경에서 read-only 및 주문 smoke test를 수행한다.
10. 운영 승인 후 feature flag와 kill switch를 단계적으로 변경한다.

## When Token Invalidation Is Required

다음 상황에서는 기존 MEMORY token을 사용하지 않아야 한다.

- app key 또는 app secret 교체
- credential 노출 의심
- REAL/DEMO 환경 설정 변경
- KIS에서 token이 강제 폐기됐다는 응답 수신

credential 교체는 런타임 환경변수 변경만으로 반영되지 않으므로 서버를
재시작해 cache를 비운다. 단순 만료 임박은 재시작 대신 수동 refresh 또는
`KIS_TOKEN_REFRESH` scheduler로 처리한다.

## Incident Response

1. kill switch를 활성화한다.
2. 미체결 주문과 취소 요청을 조회한다.
3. token 원문을 로그에 추가하지 않는다.
4. 필요하면 credential을 폐기하고 rotation 절차를 수행한다.
5. readiness의 blocking reason과 scheduler execution history를 확인한다.
6. 원인이 해소되기 전에는 kill switch를 해제하지 않는다.
