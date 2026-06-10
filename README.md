# TradeGuard

Spring Boot 기반 한국 주식 자동매매 보조 시스템입니다. MVP 1차는 한국투자증권 Open API 실주문이 아니라 모의투자, 분석, 알림 중심의 구조를 만드는 데 집중합니다.

## MVP 범위

- 관심종목 등록/조회
- 일봉 데이터 및 기술지표 저장 JPA 구조
- MA5, MA20, MA60, RSI(14), MACD, Bollinger Band 계산
- 종가베팅 후보 점수화
- 기본 RiskManager 정책
- FakeBrokerAdapter 기반 모의 주문 요청
- 중복 주문 방지와 신호/주문 요청 이력 저장 구조

## 실행

```sh
./gradlew bootRun
```

기본 실행은 H2 인메모리 DB를 사용합니다. MySQL을 사용할 때는 `.env.example`을 참고해 환경변수를 지정합니다.

DB 스키마는 Flyway migration으로 생성합니다. Hibernate는 기본적으로 `ddl-auto=validate`로 동작하므로 JPA가 테이블을 자동 생성하지 않습니다.

KIS 모의투자 일봉 조회에는 `KIS_APP_KEY`, `KIS_APP_SECRET`이 필요합니다. 100건을 초과하는 기간은 자동으로 분할 조회합니다. 구현은 모의투자 호스트만 허용하며 실제 주문 API를 호출하지 않습니다.

14:00 시장 순위와 15:00 현재가 snapshot은 기본적으로 fake adapter를 사용합니다. KIS 읽기 전용 조회로 전환하려면 `MARKET_DATA_REALTIME_PROVIDER=kis`와 `KIS_APP_KEY`, `KIS_APP_SECRET`을 설정합니다. 이 전환은 순위/현재가 조회만 활성화하며 주문 endpoint는 호출하지 않습니다.

장초반 분봉은 기본적으로 `FakeIntradayBarAdapter`를 사용합니다. KIS 읽기 전용 당일 분봉으로 전환하려면 다음과 같이 설정합니다.

```sh
MARKET_DATA_INTRADAY_PROVIDER=kis
KIS_APP_KEY=...
KIS_APP_SECRET=...
```

KIS 분봉 adapter는 공식 `주식당일분봉조회[v1_국내주식-022]`의 `/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice`, TR ID `FHKST03010200`만 사용합니다. 당일 데이터만 제공되는 endpoint이므로 과거 거래일 요청은 빈 결과를 반환하여 기존 snapshot fallback이 적용됩니다. 주문, 계좌, 잔고, 정정/취소 endpoint는 호출하지 않습니다.

## 테스트

```sh
./gradlew test
```

테스트는 H2 MySQL mode에서 Flyway migration과 Hibernate schema validation을 검증합니다. Docker가 사용 가능하면 `MySqlMigrationIntegrationTest`가 MySQL Testcontainers로 V1 migration과 핵심 unique constraint도 검증합니다.

로컬 자격증명으로 KIS 읽기 전용 smoke test를 실행하려면:

```sh
set -a
source .env
set +a
KIS_SMOKE_TEST_ENABLED=true ./gradlew test \
  --tests '*KisMarketDataSmokeTest' \
  --tests '*KisMarketRankingSmokeTest' \
  --tests '*KisMarketSnapshotSmokeTest'
```

`KIS_SMOKE_TEST_ENABLED`가 `true`가 아니면 smoke test는 실행되지 않습니다. 활성화했더라도 `KIS_APP_KEY` 또는 `KIS_APP_SECRET`이 비어 있으면 skip됩니다. smoke test는 일봉, 시장 순위, 현재가 snapshot 조회 endpoint만 호출하며 주문 endpoint는 호출하지 않습니다.

KIS 당일 분봉 전용 smoke test:

```sh
set -a
source .env
set +a
KIS_INTRADAY_BAR_SMOKE_TEST_ENABLED=true ./gradlew test \
  --tests '*KisIntradayBarSmokeTest'
```

기본 조회 종목은 삼성전자 `005930`이며 `KIS_INTRADAY_BAR_SMOKE_TEST_STOCK_CODE`로 변경할 수 있습니다. 장 시작 전에는 skip되고, 앱키나 앱시크릿이 없을 때도 skip됩니다. 이 테스트는 분봉 시세 endpoint만 호출하며 주문 API를 호출하지 않습니다.

한국 시장 휴장일은 쉼표로 구분한 ISO 날짜로 설정합니다.

```sh
MARKET_CALENDAR_HOLIDAYS=2026-01-01,2026-02-17,2026-02-18
```

주말과 설정된 휴장일에는 14:00 예비 스캔 및 15:00 최종 리뷰 scheduler가 use case를 호출하지 않습니다. 수동 스캔/리뷰 API는 휴장일에도 테스트와 디버깅을 위해 실행할 수 있습니다.

## API

관심종목 등록:

```sh
curl -X POST http://localhost:8080/api/stocks   -H 'Content-Type: application/json'   -d '{"stockCode":"005930","stockName":"삼성전자","market":"KOSPI"}'
```

관심종목 조회:

```sh
curl http://localhost:8080/api/stocks
```

특정 종목 분석 실행:

```sh
curl -X POST 'http://localhost:8080/api/analyses/005930?asOfDate=2026-06-05'
```

활성 관심종목 전체 분석 실행:

```sh
curl -X POST 'http://localhost:8080/api/analyses/active?asOfDate=2026-06-05'
```

전체 분석에서는 일봉이 60개 미만인 종목을 `SKIPPED`로 반환하고 나머지 활성 종목 분석을 계속합니다.

14:00 종가베팅 예비 후보 수동 스캔:

```sh
curl -X POST 'http://localhost:8080/api/scans/closing-bet?tradeDate=2026-06-05&limit=5'
```

스캔은 관심종목 등록 여부와 무관한 시장 순위 후보군에서 `CLOSING_BET_PRE_SCAN` 신호를 저장합니다. 기본은 fake 순위 데이터이며 `MARKET_DATA_REALTIME_PROVIDER=kis` 설정 시 KIS 읽기 전용 순위 API를 사용합니다. 자동 주문은 생성하지 않으며, 거래일 14:00 Asia/Seoul 기준으로도 실행됩니다.

15:00 종가베팅 최종 후보 수동 리뷰:

```sh
curl -X POST 'http://localhost:8080/api/reviews/closing-bet?tradeDate=2026-06-05&limit=5'
```

리뷰는 같은 거래일의 `CLOSING_BET_PRE_SCAN` 신호 중 리스크 사유가 없고 snapshot 기반 최종 점수 75점 이상인 후보를 `CLOSING_BET` 신호로 저장합니다. VWAP 상회, 당일 고가권 유지, 누적 거래대금 500억 이상을 가점하고 VWAP 하회나 고가 대비 큰 이탈을 감점합니다. 자동 주문은 생성하지 않으며, 평일 15:00 Asia/Seoul 기준으로도 실행됩니다.

08:30 장초반 예비 후보 수동 스캔:

```sh
curl -X POST 'http://localhost:8080/api/scans/early-market/pre-open?tradeDate=2026-06-10&limit=10'
```

거래대금 상위 `+20`, 양호한 등락률 `+15`, 거래량 상위 `+15`를 적용합니다. 저장된 지표가 있고 현재가가 MA5와 MA20 위이면 `+15`이며, 과열 또는 지표 부족은 reason에 기록합니다. 전일 시간외 데이터가 있으면 상승률 3% 이상 `+15`, 거래대금 300억 원 이상 `+15`, 상승률 7% 이상 과열 `-10`, 하락률 -3% 이하 `-10`을 추가 적용합니다. 시간외 데이터가 없으면 감점하지 않고 `AFTER_HOURS_DATA_UNAVAILABLE` reason을 남깁니다. 결과는 `strategyName=EARLY_MARKET_BREAKOUT`, `signalType=EARLY_MARKET_PRE_SCAN`으로 저장합니다.

시간외 데이터는 현재 `FakeAfterHoursMarketDataAdapter`만 제공합니다. 로컬 기본값은 활성화이며 아래 환경변수로 no-op adapter로 전환할 수 있습니다.

```sh
AFTER_HOURS_DATA_ENABLED=false ./gradlew bootRun
```

Fake adapter는 고정 데이터를 반환해 장초반 점수와 브리핑을 재현 가능하게 합니다. 조회일은 직전 평일이며 공휴일을 포함한 정확한 직전 거래일 계산은 KRX calendar 연동과 함께 보강할 TODO입니다. 실제 KIS 시간외 조회 endpoint adapter는 구현하지 않았으며 후속 TODO입니다.

09:05 장초반 압축 후보 수동 스캔:

```sh
curl -X POST 'http://localhost:8080/api/scans/early-market/opening?tradeDate=2026-06-10&limit=3'
```

같은 거래일의 예비 신호를 snapshot으로 재평가합니다. VWAP 위 `+25`, 당일 고가권 `+20`, 누적 거래대금 충분 `+20`, VWAP 이탈 `-30`, 고가 대비 큰 이탈 `-20`을 적용하며 70점 이상만 최대 3개 저장합니다. 결과는 `signalType=EARLY_MARKET_ENTRY_CANDIDATE`입니다.

장초반 최종 후보는 기존 signalId 기반 지정가 모의 주문 API에서 사용할 수 있습니다. `EARLY_MARKET_PRE_SCAN`은 관찰 후보이므로 주문 요청이 거절됩니다. 08:30/09:05 스캔과 scheduler는 실제 주문을 생성하지 않습니다.

09:30 이후 장초반 후보 성과 수동 캡처:

```sh
curl -X POST 'http://localhost:8080/api/scans/early-market/performances?tradeDate=2026-06-10'
```

성과 조회:

```sh
curl 'http://localhost:8080/api/scans/early-market/performances?tradeDate=2026-06-10'
curl 'http://localhost:8080/api/scans/early-market/performances/21'
```

성과는 `EARLY_MARKET_PRE_SCAN`과 `EARLY_MARKET_ENTRY_CANDIDATE`를 signalId별로 구분해 저장하며 응답에 원 신호 점수인 `signalScore`를 포함합니다. `IntradayBarPort`에서 09:00~09:30 양 끝을 포함한 1분봉을 조회하고, 첫 bar의 open을 `entryReferencePrice`로 사용합니다. 구간 high/low의 최댓값과 최솟값을 저장하고, 마지막 bar close를 `priceAt0930`으로 사용합니다. 정확히 09:30 bar가 없으면 조회 구간 안에서 가장 늦은 bar close가 사용됩니다.

`maxReturnRateUntil0930`은 `(구간 최고가 - 기준가) / 기준가 * 100`, `maxDrawdownRateUntil0930`은 `(구간 최저가 - 기준가) / 기준가 * 100`으로 계산하므로 하락 시 음수입니다. `vwapBroken`은 구간 중 하나 이상의 bar가 `close < vwap`이면 `true`입니다. 분봉이 없거나 조회가 실패하면 기존 `MarketSnapshotPort` current price proxy로 fallback하며, 이 경우 구간 기반 필드는 `null`, `priceAt0930`과 `vwapBroken`만 snapshot으로 채웁니다. snapshot도 없으면 nullable 필드는 그대로 `null`입니다.

분봉 provider 기본값은 `fake`이며 상승/하락/횡보 고정 시나리오를 제공합니다. `MARKET_DATA_INTRADAY_PROVIDER=kis`이면 KIS 당일 1분봉을 최대 30건씩 역방향 조회해 요청 구간을 구성합니다. KIS 응답의 `cntg_vol`을 분당 거래량으로, `acml_tr_pbmn`의 직전 분 대비 차이를 분당 거래대금으로 사용하며, 분당 VWAP은 `분당 거래대금 / 분당 거래량`으로 계산합니다. 무체결로 VWAP을 계산할 수 없는 row는 bar에서 제외하므로 `IntradayBar.vwap`은 non-null 정책을 유지합니다.

`FIVE_MINUTES` 요청은 1분봉을 5분 버킷으로 집계합니다. open은 첫 bar, high/low는 구간 최댓값/최솟값, close는 마지막 bar, volume/tradingValue는 합계, VWAP은 `합산 거래대금 / 합산 거래량`입니다. 실제 주문이나 시장가 주문은 수행하지 않습니다.

장초반 성과는 평일 Asia/Seoul 기준 09:31에 `EARLY_MARKET_PERFORMANCE_CAPTURE_930` scheduler가 자동 캡처합니다. 09:30 bar가 완료된 뒤 실행하기 위해 09:31을 사용하며, `MarketCalendarPort`가 비거래일로 판단하면 `NON_TRADING_DAY` 사유로 건너뜁니다. 거래일에는 수동 API와 동일한 `CaptureEarlyMarketPerformancesUseCase`를 호출하므로 저장 및 fallback 정책이 동일합니다.

자동 캡처 후 Discord에는 후보 수, 캡처 성공 수, `bars_used`/`snapshot_proxy` 수, `vwapBroken` 후보 수, `maxReturnRateUntil0930` 상위 3개를 요약합니다. webhook 미설정 시 알림은 no-op이며 scheduler 자체는 성공하고 `notificationSent=false`로 기록됩니다.

거래 신호 조회:

```sh
curl 'http://localhost:8080/api/signals?stockCode=005930&signalDate=2026-06-05&strategyName=CLOSING_BET&signalType=BUY_CANDIDATE&status=CREATED&minScore=70'
```

응답에는 `signalId`, 전략명, 종목코드, 신호일, 신호 유형, 점수, 점수 근거, 리스크 거절 사유, 상태가 포함됩니다.

저장된 분석 신호로 모의 주문 요청:

```sh
curl -X POST http://localhost:8080/api/mock-orders \
  -H 'Content-Type: application/json' \
  -d '{
    "strategyName":"CLOSING_BET",
    "stockCode":"005930",
    "signalDate":"2026-06-05",
    "signalType":"BUY_CANDIDATE",
    "quantity":1,
    "limitPrice":50000
  }'
```

`signalId`로 모의 주문 요청:

```sh
curl -X POST http://localhost:8080/api/signals/1/mock-orders \
  -H 'Content-Type: application/json' \
  -d '{
    "quantity":1,
    "limitPrice":50000
  }'
```

모의 주문 이력 조회:

```sh
curl 'http://localhost:8080/api/mock-orders?stockCode=005930&tradeDate=2026-06-05&status=ACCEPTED&side=BUY'
```

특정 TradingSignal의 주문 이력 조회:

```sh
curl 'http://localhost:8080/api/mock-orders?signalId=1'
```

Broker 실패 주문 조회:

```sh
curl 'http://localhost:8080/api/mock-orders?status=BROKER_FAILED'
```

Broker 실패 주문 수동 재시도:

```sh
curl -X POST 'http://localhost:8080/api/mock-orders/10/retry'
```

5분 이상 정체된 재시도 조회:

```sh
curl 'http://localhost:8080/api/mock-orders/retries/stuck?thresholdMinutes=5'
```

정체된 재시도를 수동으로 `BROKER_FAILED`로 복구:

```sh
curl -X POST 'http://localhost:8080/api/mock-orders/10/retry/recover' \
  -H 'Content-Type: application/json' \
  -d '{
    "reason":"application restarted during retry"
  }'
```

TradingSignal 상태 변경 이력 조회:

```sh
curl 'http://localhost:8080/api/signals/21/histories'
```

OrderRequest 상태 변경 이력 조회:

```sh
curl 'http://localhost:8080/api/mock-orders/10/histories'
```

모의 주문 API는 DB에 저장된 신호만 사용하며 지정가 주문만 생성합니다. 주문 이력 응답에는 `orderId`, `signalId`, `failureReason`, `failedAt`, `retryable`, `retryRequestedAt`이 포함됩니다. signalId 기반 및 논리 키 기반 주문 모두 조회된 TradingSignal ID를 `order_requests.signal_id`에 저장합니다. Broker 호출 중 예외가 발생하면 주문은 `BROKER_FAILED`로 저장되고 `brokerOrderNo`는 null로 유지됩니다. 필터를 생략하면 전체 주문 이력을 최신 거래일 순으로 반환합니다.

Broker 실패 시 POST 응답의 `approved`는 `false`, `brokerFailed`는 `true`이며 실패 사유를 `failureReason`으로 반환합니다. 리스크 승인은 완료되었지만 Broker 요청이 성공하지 않았으므로 TradingSignal은 `RISK_APPROVED` 상태를 유지합니다.

수동 재시도는 `BROKER_FAILED`이면서 `retryable=true`인 주문만 허용합니다. 재시도는 새로운 주문 row를 만들지 않고 기존 orderId row를 `RETRY_REQUESTED`로 원자적으로 선점합니다. 자동 재시도는 수행하지 않습니다.

재시도가 성공하고 주문에 `signalId`가 연결되어 있으면 해당 TradingSignal을 `ORDER_REQUESTED`로 동기화합니다. V3 이전에 생성되어 `signalId`가 null인 기존 주문은 신호 동기화를 건너뛰고 주문 재시도 자체는 정상 처리합니다.

`RETRY_REQUESTED` 전환 시 `retryRequestedAt`을 기록합니다. 기본 5분 이상 정체되면 운영 조회 대상이며, 복구 API는 상태를 `BROKER_FAILED`로 되돌리고 실패 사유/시각을 갱신한 뒤 `retryable=true`를 유지합니다. 기준은 `tradeguard.order.retry-stuck-threshold-minutes` 또는 `ORDER_RETRY_STUCK_THRESHOLD_MINUTES`로 변경할 수 있습니다. 자동 정체 복구 scheduler는 없습니다.

상태 변경 이력 응답은 대상 ID, `fromStatus`, `toStatus`, `reason`, `createdAt`을 시간순으로 반환합니다. 감사 이력은 기존 상태 전이를 대체하지 않으며, 주요 신호 승인/주문 요청과 Broker 성공·실패·수동 재시도·stuck 복구 전이를 추적합니다.

종가베팅 브리핑 알림:

```sh
curl -X POST 'http://localhost:8080/api/briefings/closing-bet?signalDate=2026-06-05'
```

`DISCORD_WEBHOOK_URL`이 설정되어 있으면 Discord Webhook으로 종가베팅 후보 브리핑을 전송합니다. 비어 있으면 실제 전송 없이 no-op 결과를 반환합니다. 알림은 정보 전달만 수행하며 주문을 실행하지 않습니다.

공통 오류 응답:

```json
{
  "code": "INVALID_REQUEST",
  "message": "quantity must be greater than or equal to 1"
}
```

저장된 신호가 없으면 `TRADING_SIGNAL_NOT_FOUND`를 반환합니다.

## 운영 Health 확인

Actuator는 `health`, `info`, `metrics` endpoint를 외부에 노출하며 health 상세정보는 기본적으로 반환하지 않습니다.

```sh
curl 'http://localhost:8080/actuator/health'
curl 'http://localhost:8080/actuator/health/liveness'
curl 'http://localhost:8080/actuator/health/readiness'
curl 'http://localhost:8080/actuator/info'
curl 'http://localhost:8080/actuator/metrics'
```

- `liveness`: Spring 애플리케이션 생존 상태만 확인한다.
- `readiness`: 애플리케이션 readiness, DB, Flyway, KIS read-only 설정, Discord 설정, 14:00/15:00 및 장초반 08:30/09:05/09:31 scheduler와 시장 calendar bean을 확인한다.
- DB는 Spring Boot 기본 DataSource health를 사용한다.
- Flyway pending migration이 있으면 `flywayMigration`이 `DOWN`이다. migration 자체가 실패하면 애플리케이션 시작이 실패하므로 readiness endpoint가 열리지 않는다.
- KIS provider가 `fake`이면 `UP`, `kis`이면서 자격증명이 없으면 `UNKNOWN`, 자격증명이 구성되면 설정 기준 `UP`이다.
- Discord webhook 미설정은 `UNKNOWN`, 설정됨은 `UP`이다.
- KIS/Discord health는 외부 API 호출이나 메시지 전송을 수행하지 않는다.
- API Key, App Secret, Discord webhook URL은 health 응답에 포함하지 않는다.

## Scheduler 실행 이력

14:00 예비 스캔, 15:00 최종 리뷰, 장초반 08:30/09:05 스캔 및 09:31 성과 캡처의 자동 scheduler 실행 이력을 조회할 수 있습니다.

```sh
curl 'http://localhost:8080/api/scheduler-executions'
```

거래일과 scheduler를 지정한 조회:

```sh
curl 'http://localhost:8080/api/scheduler-executions?tradeDate=2026-06-05&schedulerName=CLOSING_BET_PRE_SCAN_14'
```

실패 실행 조회:

```sh
curl 'http://localhost:8080/api/scheduler-executions?status=FAILED'
```

실행 상태는 `STARTED`, `SUCCEEDED`, `SKIPPED`, `FAILED`이며 최신 `startedAt` 순으로 반환됩니다. 비거래일에는 `SKIPPED`와 `NON_TRADING_DAY` 사유를 기록합니다. 실행 예외가 발생하면 `FAILED`를 저장한 뒤 예외를 다시 전파합니다.

응답의 `scannedCount`는 14:00에는 시장 후보군 수, 15:00에는 재검토한 pre-scan 후보 수, 09:31 성과 캡처에는 대상 signal 수를 의미합니다. 09:31의 `selectedCount`는 캡처 성공 수입니다. `selectedCount`와 Discord 브리핑 전송 여부인 `notificationSent`도 함께 기록합니다. 수동 scan/review/capture API 호출은 scheduler 실행 이력에 포함하지 않습니다.

운영자는 매 거래일 아래 항목을 확인할 수 있습니다.

- 14:00 `CLOSING_BET_PRE_SCAN_14`가 `SUCCEEDED` 또는 의도된 `SKIPPED`인지
- 15:00 `CLOSING_BET_FINAL_REVIEW_15`가 실행됐는지
- `FAILED` 실행의 `failureReason`
- 후보 스캔/선정 수와 Discord 알림 전송 여부

## 운영 Metrics와 요청 추적

기본 Micrometer registry로 아래 counter를 기록합니다. Prometheus/Grafana 의존성은 포함하지 않습니다.

- `tradeguard.scheduler.execution.count`: `schedulerName`, `status`
- `tradeguard.scheduler.selected.count`: `schedulerName`
- `tradeguard.scheduler.notification.sent.count`: `schedulerName`, `sent`
- `tradeguard.order.request.count`: `status`
- `tradeguard.order.broker_failure.count`: `retryable`
- `tradeguard.order.retry.count`: `result`
- `tradeguard.order.retry_recovery.count`: `result`
- `tradeguard.notification.discord.count`: `result`
- `tradeguard.kis.read_only.count`: `operation`, `result`
- `tradeguard.after_hours.lookup.count`: `result=found|not_found|failure`
- `tradeguard.intraday_bar.lookup.count`: `result=found|not_found|failure`
- `tradeguard.early_market.performance.capture.count`: `result=bars_used|snapshot_proxy|failed`

장초반 scheduler는 기존 scheduler metric에 다음 `schedulerName` tag로 기록됩니다.

- `EARLY_MARKET_PRE_OPEN_830`
- `EARLY_MARKET_OPENING_905`
- `EARLY_MARKET_PERFORMANCE_CAPTURE_930`

장초반 scheduler는 평일 Asia/Seoul 기준 08:30, 09:05, 09:31에 실행하며 `MarketCalendarPort`가 비거래일로 판단하면 `SKIPPED` 이력을 남깁니다. 거래일에는 `STARTED` 후 `SUCCEEDED` 또는 `FAILED`로 전환하고 후보 또는 캡처 수와 Discord 전송 여부를 저장합니다.

개별 metric 조회 예시:

```sh
curl 'http://localhost:8080/actuator/metrics/tradeguard.scheduler.execution.count'
curl 'http://localhost:8080/actuator/metrics/tradeguard.order.broker_failure.count'
```

HTTP 요청에 `X-Request-Id`가 있으면 정제 후 MDC와 응답 헤더에 사용하고, 없으면 UUID를 생성합니다. API에서 발생한 신호/주문 상태 변경 감사 이력에는 동일 값을 `requestCorrelationId`로 저장하고 `actor=API`를 기록합니다. stuck retry 복구 이력은 `actor=SYSTEM`입니다.

Scheduler 실행은 매 실행마다 별도 correlation ID를 생성합니다. 같은 ID가 MDC, 구조화 로그, `scheduler_execution_histories.correlation_id`에 사용되며 조회 API의 `correlationId`로 반환됩니다.

```sh
curl -i -X POST 'http://localhost:8080/api/signals/21/mock-orders' \
  -H 'X-Request-Id: operation-20260609-001' \
  -H 'Content-Type: application/json' \
  -d '{"quantity":1,"limitPrice":50000}'

curl 'http://localhost:8080/api/signals/21/histories'
curl 'http://localhost:8080/api/mock-orders/10/histories'
curl 'http://localhost:8080/api/scheduler-executions?tradeDate=2026-06-09'
```

운영자는 응답의 `X-Request-Id`, 감사 이력의 `requestCorrelationId`, scheduler 이력의 `correlationId`로 구조화 로그를 검색해 하나의 실행 흐름을 추적할 수 있습니다.

API Key, App Secret, Discord webhook URL, 계좌번호, 종목코드는 metric tag로 사용하지 않습니다. HTTP 로그에는 query string을 남기지 않으며 KIS/Discord 로그에는 URL이나 자격증명을 기록하지 않습니다.
Correlation ID는 metric tag로 사용하지 않습니다.

## 안전 원칙

- 실계좌 주문 기능은 구현하지 않습니다.
- 시장가 주문은 지원하지 않습니다.
- 08:30/09:05 장초반 후보 생성과 09:31 성과 캡처는 자동 주문을 실행하지 않습니다.
- 장초반 성과 캡처는 분석 데이터만 저장하며 주문을 생성하지 않습니다.
- 시간외 데이터 연동은 fake/no-op adapter만 제공하며 KIS 실제 시간외 endpoint는 호출하지 않습니다.
- API Key, App Secret, 계좌번호는 코드에 하드코딩하지 않습니다.
- Discord Webhook URL은 환경변수로만 주입하며 코드에 하드코딩하지 않습니다.
- `KisBrokerAdapter`는 스켈레톤만 제공하며 실제 주문 API를 호출하지 않습니다.
