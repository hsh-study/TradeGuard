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

테스트는 H2 MySQL mode에서 Flyway migration과 Hibernate schema validation을 검증합니다. Docker가 사용 가능하면 `MySqlMigrationIntegrationTest`가 MySQL Testcontainers로 V1~V12 migration과 핵심 unique constraint도 검증합니다.

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

KIS 시간외 일별 데이터 smoke test:

```sh
set -a
source .env
set +a
KIS_AFTER_HOURS_SMOKE_TEST_ENABLED=true ./gradlew test \
  --tests '*KisAfterHoursSmokeTest'
```

기본 종목은 삼성전자 `005930`이며 `KIS_AFTER_HOURS_SMOKE_TEST_STOCK_CODE`로 변경할 수 있습니다. 앱키나 앱시크릿이 없으면 skip됩니다. 조회일은 현재 직전 평일이며 공휴일 직후에는 데이터가 없어 실패할 수 있습니다. 시간외 시세 endpoint만 호출하며 주문, 계좌, 잔고, 정정/취소 API는 호출하지 않습니다.

한국 시장 calendar는 `market_calendar_days`에 연중 모든 날짜를 저장합니다. 운영 source 우선순위는 다음과 같습니다.

1. `MANUAL_OVERRIDE`
2. `KRX_OFFICIAL`
3. `FALLBACK_GENERATED`
4. DB 날짜가 없을 때 runtime 주말 + `MARKET_CALENDAR_HOLIDAYS` fallback

동일 날짜는 하나의 row만 저장되며, `MANUAL_OVERRIDE` row는 이후 KRX/fallback 재동기화로 덮어쓰지 않습니다. Runtime fallback용 수동 휴장일은 쉼표로 구분한 ISO 날짜로 설정합니다.

```sh
MARKET_CALENDAR_HOLIDAYS=2026-01-01,2026-02-17,2026-02-18
```

DB에 날짜가 있으면 `MarketCalendarPort`는 저장된 `tradingDay`를 사용합니다. DB 범위가 없거나 불완전하면 warning log를 남기고 주말과 `MARKET_CALENDAR_HOLIDAYS`를 사용합니다. 14:00/15:00 및 장초반 08:30/09:05/09:20/09:31 scheduler skip, 08:30 시간외 기준일, 이전/다음 거래일과 기간 리포트의 `tradingDayCount`가 같은 calendar를 사용합니다.

Calendar 동기화:

```sh
curl -X POST 'http://localhost:8080/api/market-calendar/sync?year=2026'
curl 'http://localhost:8080/api/market-calendar/days?from=2026-01-01&to=2026-12-31'
```

동기화 응답은 `syncedCount`, `tradingDayCount`, `holidayCount`, `source`, `warnings`를 반환합니다. `source`는 `KRX_OFFICIAL` 또는 `FALLBACK_GENERATED`입니다.

현재 KRX 정보데이터시스템에는 운영 코드에서 직접 의존할 수 있는 안정적인 무인증 calendar endpoint가 명확히 문서화되어 있지 않습니다. 따라서 `KrxMarketCalendarSyncProvider`의 client/parser 경계는 구현했지만 기본 `MARKET_CALENDAR_KRX_ENDPOINT`는 비어 있습니다. `{year}` placeholder를 포함한 검증된 JSON endpoint를 운영자가 설정하지 않으면 공식 호출은 명시적으로 실패하고, 주말, 수동 휴일, 5월 1일, 연말 최종 영업일 휴장을 반영한 `FALLBACK_GENERATED` calendar가 저장됩니다. fallback은 법정 공휴일과 임시 휴장일을 자동 판별하지 않으므로 `MARKET_CALENDAR_HOLIDAYS`를 manual override로 계속 관리해야 합니다.

Calendar 수동 보정:

```sh
curl -X PATCH 'http://localhost:8080/api/market-calendar/days/2026-08-17' \
  -H 'Content-Type: application/json' \
  -d '{
    "market": "KRX_STOCK",
    "tradingDay": false,
    "holidayName": "TEMPORARY_CLOSURE",
    "reason": "KRX 공지 기준 임시 휴장 보정",
    "actor": "operator"
  }'
```

기존 row가 없으면 생성하고, 있으면 갱신합니다. `reason`은 필수이며 `actor`가 없으면 `MANUAL_API`를 사용합니다. 저장 source는 `MANUAL_OVERRIDE`가 되고 before/after 값은 `market_calendar_day_audits`에 기록됩니다.

Calendar 검증 및 audit 조회:

```sh
curl 'http://localhost:8080/api/market-calendar/validation?year=2026'
curl 'http://localhost:8080/api/market-calendar/audits?from=2026-01-01&to=2026-12-31'
```

검증 응답은 연중 누락 날짜, 주말 거래일, 평일 휴장일, source별 개수와 향후 30일 거래일 존재 여부 warning을 제공합니다. Audit은 `createdAt`, `id` 최신순입니다. 동기화, 검증, 수동 보정은 calendar 기준정보만 관리하며 자동 주문을 실행하지 않습니다.

거래일 계산 운영 확인:

```sh
curl 'http://localhost:8080/api/market-calendar/trading-days?date=2026-02-19'
```

응답에는 요청일의 `tradingDay`, 입력일을 제외한 `previousTradingDay`와 `nextTradingDay`가 포함됩니다.

## API

### KIS 수동 승인형 실매매

Live trading은 기본 비활성입니다. 신규 주문은 아래 조건을 모두 만족할 때만 허용됩니다.

- `LIVE_TRADING_ENABLED=true`
- `KIS_TRADING_ENABLED=true`
- 계좌번호와 계좌상품코드 설정
- DB kill switch 비활성
- `MarketCalendarPort` 기준 거래일의 09:00~15:30
- `LIMIT` 지정가 주문
- 주문금액이 `LIVE_MAX_ALLOWED_ORDER_AMOUNT` 이하

실전은 `KIS_TRADING_ENVIRONMENT=REAL`과 공식 실전 host, 모의는 `DEMO`와 모의 host 조합만 허용합니다. KIS 공식 현금주문 endpoint `/uapi/domestic-stock/v1/trading/order-cash`를 사용하며 실전 매수/매도 TR_ID는 `TTTC0012U`/`TTTC0011U`, 모의는 `VTTC0012U`/`VTTC0011U`입니다. KRX 직접 주문 API는 사용하지 않습니다.

수동 지정가 매수:

```sh
curl -X POST 'http://localhost:8080/api/live-orders/buy' \
  -H 'Content-Type: application/json' \
  -d '{"signalId":21,"stockCode":"005930","quantity":1,
       "orderPrice":70000,"orderType":"LIMIT"}'
```

수동 지정가 매도:

```sh
curl -X POST 'http://localhost:8080/api/live-orders/sell' \
  -H 'Content-Type: application/json' \
  -d '{"positionId":3,"quantity":1,"orderPrice":73500,
       "reason":"MANUAL_EXIT"}'
```

조회와 예상 순손익:

```sh
curl 'http://localhost:8080/api/live-orders?status=ACCEPTED'
curl 'http://localhost:8080/api/live-orders/10'
curl 'http://localhost:8080/api/live-orders/10/histories'
curl 'http://localhost:8080/api/live-orders/open'
curl 'http://localhost:8080/api/live-orders/10/fills'
curl 'http://localhost:8080/api/live-orders/10/cancel-requests'
curl 'http://localhost:8080/api/live-positions'
curl 'http://localhost:8080/api/live-positions/3/exit-preview?currentPrice=73500'
```

미체결 지정가 주문 취소:

```sh
curl -X POST 'http://localhost:8080/api/live-orders/10/cancel' \
  -H 'Content-Type: application/json' \
  -d '{"reason":"OPERATOR_CANCEL","cancelQuantity":1}'
```

`cancelQuantity`을 생략하면 저장된 미체결 잔량 전체를 취소합니다. `ACCEPTED` 또는 `PARTIALLY_FILLED` 주문만 취소할 수 있으며 kill switch가 켜져 있어도 기존 주문의 위험 축소를 위한 취소는 허용됩니다. 취소에는 `KIS_TRADING_ENABLED=true`와 유효한 계좌/환경 설정이 필요합니다. KIS 공식 현금주문 정정취소 endpoint와 실전/모의 취소 TR ID `TTTC0013U`/`VTTC0013U`를 사용합니다. 정정 주문은 아직 지원하지 않습니다.

순손익은 `매도금액 - 매도세금 - 매도수수료 - 매수금액 - 매수수수료`이며 익절은 순수익률 기준입니다. 기본 threshold는 매수가 5만원 미만 `+5%/-3%`, 5만~20만원 `+4%/-2.5%`, 20만원 이상 `+3%/-2%`입니다. `maxLossAmount` 도달은 손절률보다 우선합니다.

`LIVE_POSITION_EXIT_MONITOR`는 두 feature flag가 켜진 장중에 1분마다 OPEN 포지션을 평가합니다. 매도는 현재가 지정가만 사용하며 `SELL_ORDERED` 포지션에는 중복 주문하지 않습니다. 주문 실패 시 포지션은 OPEN을 유지합니다. 자동매수는 구현하지 않았습니다.

`LIVE_ORDER_RECONCILIATION`은 `KIS_TRADING_ENABLED=true`일 때 평일 1분마다 `ACCEPTED`/`PARTIALLY_FILLED` 주문의 누적 체결량과 잔량을 조회합니다. 새로 늘어난 체결 수량만 fill로 저장하고 매수 평균가/수량 또는 매도 잔여 포지션에 반영합니다. 전량 체결은 `FILLED`, 일부 체결은 `PARTIALLY_FILLED`로 전환됩니다.

자동취소 기본값은 비활성입니다.

- `LIVE_ORDER_AUTO_CANCEL_ENABLED=false`
- `LIVE_BUY_ORDER_EXPIRE_MINUTES=3`
- `LIVE_SELL_ORDER_EXPIRE_MINUTES=3`
- `LIVE_CANCEL_BEFORE_MARKET_CLOSE_MINUTES=5`

자동취소를 명시적으로 켠 경우에만 주문별 만료시간 또는 장 마감 임박 조건으로 미체결 잔량을 취소합니다. 시장가 주문, 자동매수, 공매도, 신용/미수 주문은 계속 지원하지 않습니다.

Kill switch:

```sh
curl -X POST 'http://localhost:8080/api/live-trading/kill-switch' \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true,"reason":"OPERATOR_EMERGENCY_STOP"}'
```

Kill switch가 켜지면 수동 매수·매도와 자동매도 신규 주문을 모두 차단합니다. 이미 접수된 주문 취소와 reconciliation은 위험 축소 목적이므로 허용됩니다.

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

시간외 데이터 provider는 `fake`, `kis`, `disabled`를 지원하며 기본값은 `fake`입니다.

```sh
AFTER_HOURS_DATA_PROVIDER=kis ./gradlew bootRun
AFTER_HOURS_DATA_PROVIDER=disabled ./gradlew bootRun
```

새 `AFTER_HOURS_DATA_PROVIDER`가 설정되면 이를 우선합니다. 기존 `AFTER_HOURS_DATA_ENABLED=false` 설정도 provider가 비어 있을 때 `disabled`로 해석되므로 하위 호환됩니다. 기존 true 또는 미설정은 `fake`입니다.

Fake adapter는 고정 데이터를 반환해 장초반 점수와 브리핑을 재현 가능하게 합니다. KIS provider는 공식 `주식현재가 시간외일자별주가[v1_국내주식-026]` endpoint `/uapi/domestic-stock/v1/quotations/inquire-daily-overtimeprice`, TR ID `FHPST02320000`을 사용합니다. 최근 일별 시간외 단일가 데이터에서 요청 거래일을 찾아 가격, 전일 대비율, 거래량, 거래대금을 매핑합니다. 종목명이 응답에 보장되지 않아 `stockName`은 종목코드로 대체합니다.

08:30 스캔의 시간외 조회 기준일은 `MarketCalendarPort.previousTradingDay(tradeDate)`로 계산합니다. 주말과 `MARKET_CALENDAR_HOLIDAYS`의 연휴/임시휴장일을 건너뛰며 신호 reason에 `AFTER_HOURS_TRADE_DATE_yyyy-MM-dd`를 남깁니다. 요청일이 KIS 최근 데이터에 없으면 빈 결과를 반환하고 `AFTER_HOURS_DATA_UNAVAILABLE` reason을 남깁니다. KIS endpoint는 종목별 조회이므로 `findTopAfterHoursMovers`의 시장 전체 순위 조회는 지원하지 않고 빈 결과를 반환합니다.

09:05 장초반 압축 후보 수동 스캔:

```sh
curl -X POST 'http://localhost:8080/api/scans/early-market/opening?tradeDate=2026-06-10&limit=3'
```

같은 거래일의 예비 신호를 snapshot으로 재평가합니다. VWAP 위 `+25`, 당일 고가권 `+20`, 누적 거래대금 충분 `+20`, VWAP 이탈 `-30`, 고가 대비 큰 이탈 `-20`을 적용하며 70점 이상만 최대 3개 저장합니다. 결과는 `signalType=EARLY_MARKET_ENTRY_CANDIDATE`입니다.

저장된 `DailyPrice`에서 `MarketCalendarPort.previousTradingDay(tradeDate)`의 전일 고가를 조회하고, `IntradayBarPort`의 09:00~09:05 분봉으로 전일 고가 돌파와 시초가 지지를 추가 평가합니다. 구간 high가 전일 high 이상이면 `+15`, 아니면 `-10`이며 마지막 close가 첫 bar open 이상이면 `+10`, 아니면 `-15`입니다. 전일 일봉 또는 분봉이 없으면 감점하지 않고 `PRICE_ACTION_DATA_INSUFFICIENT`와 구체적인 누락 reason을 남깁니다.

장초반 최종 후보는 기존 signalId 기반 지정가 모의 주문 API에서 사용할 수 있습니다. `EARLY_MARKET_PRE_SCAN`은 관찰 후보이므로 주문 요청이 거절됩니다. 08:30/09:05 스캔과 scheduler는 실제 주문을 생성하지 않습니다.

09:20 장초반 후보 follow-up 수동 실행:

```sh
curl -X POST 'http://localhost:8080/api/scans/early-market/follow-up?tradeDate=2026-06-10'
```

같은 거래일의 `EARLY_MARKET_ENTRY_CANDIDATE`를 별도 신호 저장 없이 재평가합니다. `IntradayBarPort`에서 09:05~09:20 1분봉을 조회하며 마지막 가격이 마지막 VWAP 아래이거나 구간 고점 대비 낙폭이 -2% 이하이면 `EXCLUDE`입니다. 구간 중 `close < vwap`이 있었지만 회복했거나 낙폭이 -1%~-2%이면 `CAUTION`, 그 외 VWAP과 고가권을 유지하면 `KEEP`입니다.

동시에 09:00~09:20 가격 행동 feature를 계산합니다. 마지막 가격이 시초가 아래이면 기존 분류와 관계없이 `EXCLUDE`, 전일 고가를 돌파한 뒤 다시 아래로 내려오면 `CAUTION`, 아직 전일 고가를 돌파하지 못한 경우도 `CAUTION`입니다. 전일 고가 위를 유지하면 `PREVIOUS_HIGH_HELD`, 시초가 아래 눌림 뒤 회복하면 `PULLBACK_RECOVERED` reason을 남기며 기존 VWAP/낙폭 판단이 KEEP이면 유지합니다.

분봉이 없거나 조회가 실패하면 `MarketSnapshotPort`로 fallback합니다. snapshot의 현재가, 당일 고가, VWAP으로 같은 기준을 적용하고 `SNAPSHOT_PROXY` reason을 남깁니다. 필요한 값이 부족하면 `DATA_INSUFFICIENT`와 함께 보수적으로 `CAUTION` 처리합니다.

평일 Asia/Seoul 기준 09:20에는 `EARLY_MARKET_FOLLOW_UP_920` scheduler가 같은 use case를 자동 실행합니다. 비거래일에는 `NON_TRADING_DAY`로 건너뛰고, 거래일에는 후보 수와 KEEP/CAUTION/EXCLUDE 수를 구조화 로그에 기록합니다. Discord에는 분류 수, 점수 기준 상위 KEEP 3개, EXCLUDE 후보와 사유를 전송합니다. webhook 미설정은 no-op이며 follow-up은 신호나 주문을 새로 생성하지 않습니다.

수동 API와 scheduler는 동일한 follow-up use case에서 결과를 `signalId`별로 저장합니다. 같은 signalId를 다시 평가하면 기존 row의 decision, 가격, VWAP 이탈, reasons와 캡처 시각을 update합니다. 후보 하나라도 저장에 실패하면 follow-up 실행을 실패로 전파하고 Discord는 전송하지 않으며, scheduler 실행 이력은 `FAILED`가 됩니다.

저장된 follow-up 결과 조회:

```sh
curl 'http://localhost:8080/api/scans/early-market/follow-up-results?tradeDate=2026-06-10'
curl 'http://localhost:8080/api/scans/early-market/follow-up-results/21'
```

응답에는 `signalId`, `decision`, 신호 점수, 마지막 가격, 09:05 이후 고가, 고점 대비 낙폭, VWAP 이탈 여부, reasons와 `capturedAt`이 포함됩니다. 결과 저장과 조회는 분석 용도이며 자동 주문을 생성하지 않습니다.

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

### 장초반 원천 데이터 아카이브

08:30/09:05/09:20/09:31 scheduler는 전략 실행 전에 당시 조회 가능한 원천 데이터를 별도 테이블에 보존합니다.

- 08:30: 시장 순위 universe와 직전 거래일 시간외 시세
- 09:05: 09:00~09:05 1분봉과 opening snapshot
- 09:20: 09:05~09:20 1분봉, 분봉 부재 시 follow-up snapshot
- 09:31: 09:00~09:30 1분봉, 분봉 부재 시 performance snapshot

```sh
curl 'http://localhost:8080/api/early-market/data-captures?tradeDate=2026-06-10'
curl 'http://localhost:8080/api/early-market/ranking-snapshots?tradeDate=2026-06-10'
curl 'http://localhost:8080/api/early-market/after-hours-snapshots?tradeDate=2026-06-10'
curl 'http://localhost:8080/api/early-market/intraday-bars?tradeDate=2026-06-10&stockCode=005930'
curl 'http://localhost:8080/api/early-market/market-snapshots?tradeDate=2026-06-10&stockCode=005930'
```

동일한 `(tradeDate, stockCode, barTime, intervalType)` bar와 동일 시각·유형의 market snapshot은 upsert합니다. 캡처 이력은 거래일·capture type별로 `SUCCEEDED`, `PARTIAL`, `FAILED`, `SKIPPED` 상태와 item count, 제한된 실패 사유를 저장합니다.

원천 데이터 저장 실패는 warning log와 `tradeguard.early_market.data_capture.count` metric에 남기되 기존 전략 신호·follow-up·성과 계산은 가능한 경우 계속 실행합니다. raw 외부 응답은 info log에 남기지 않습니다. replay 백테스트는 이 데이터가 충분히 축적된 뒤 구현하며, 현재 아카이브와 조회 API는 주문을 생성하지 않습니다.

장초반 전략 일별 성과 리포트:

```sh
curl 'http://localhost:8080/api/reports/early-market/daily?tradeDate=2026-06-10'
```

리포트는 해당 거래일의 `EARLY_MARKET_PRE_SCAN`, `EARLY_MARKET_ENTRY_CANDIDATE` 신호와 저장된 `EarlyMarketCandidatePerformance`를 signalId로 결합합니다. 후보 수, 성과 캡처 수, 평균 최대수익률, 평균 최대낙폭, 최고/최저 후보와 전체 후보 상세를 반환합니다. `bestCandidate`는 `maxReturnRateUntil0930`이 가장 큰 후보, `worstCandidate`는 가장 작은 후보입니다.

그룹은 signal type, 점수 구간 `70-79`/`80-89`/`90+`, `vwapBroken`, 전일 고가 돌파, 시초가 지지, follow-up decision별로 후보 수와 성과 수, 평균 최대수익률/최대낙폭을 제공합니다. price action 그룹은 신호 reason의 `PREVIOUS_HIGH_BROKEN`, `PREVIOUS_HIGH_NOT_BROKEN`, `OPENING_PRICE_HELD`, `OPENING_PRICE_LOST`를 사용하며 reason이 없으면 `UNKNOWN`입니다. `byFollowUpDecision`은 저장된 `KEEP`, `CAUTION`, `EXCLUDE`를 사용하고 결과가 없으면 `UNKNOWN`입니다.

성과 레코드가 없는 후보는 `excludedFromPerformanceCount`에 포함됩니다. 성과가 저장됐더라도 수익률 또는 낙폭이 `null`이면 해당 평균 표본에서 제외하며 `dataCompleteness`의 표본 수로 확인할 수 있습니다. 리포트는 저장된 follow-up 결과만 결합하며 follow-up을 재실행하거나 Discord를 전송하지 않습니다. 이 조회 API는 주문을 생성하지 않습니다.

장초반 전략 기간 성과 리포트:

```sh
curl 'http://localhost:8080/api/reports/early-market/period?from=2026-06-01&to=2026-06-10'
```

`from`, `to`는 필수이며 양 끝 날짜를 포함합니다. `from`이 `to`보다 늦거나 포함 기간이 90일을 초과하면 `400 INVALID_REQUEST`를 반환합니다. `tradingDayCount`와 `byTradeDate`에는 장초반 후보 신호가 존재한 날짜만 포함하고, 응답 크기를 제한하기 위해 날짜별 후보 상세는 반환하지 않습니다.

기간 평균과 signal type, 점수 구간, VWAP 이탈, 전일 고가 돌파, 시초가 지지, follow-up decision 그룹은 기간의 전체 후보를 다시 합산해 계산합니다. 따라서 일별 평균의 단순 평균이 아니라 수익률 값이 존재하는 후보 단위 평균입니다. `winRate`는 `maxReturnRateUntil0930 > 0`인 후보 수를 수익률 값이 존재하는 후보 수로 나눈 백분율이며 소수점 넷째 자리까지 반환합니다. 수익률이 `null`인 후보는 승률 표본에서 제외되고 `dataCompleteness.winSampleCount`, `winCount`로 표본을 확인할 수 있습니다.

기간 리포트는 저장 데이터 조회와 전략 검증 전용입니다. follow-up, 성과 캡처, 모의 주문 또는 자동 주문을 실행하지 않으며 실계좌 주문과 시장가 주문을 지원하지 않습니다.

### 장초반 전략 파라미터

장초반 전략 설정 prefix는 `tradeguard.early-market.strategy`입니다. 기본값은 기존 08:30, 09:05, 09:20 정책과 동일합니다.

| 그룹 | 설정 | 기본값 |
| --- | --- | ---: |
| `pre-open` | `after-hours-rise-threshold` | `3.0` |
| `pre-open` | `after-hours-rise-score` | `15` |
| `pre-open` | `after-hours-trading-value-threshold` | `30000000000` (300억 원) |
| `pre-open` | `after-hours-trading-value-score` | `15` |
| `pre-open` | `after-hours-overheat-threshold` | `7.0` |
| `pre-open` | `after-hours-overheat-penalty` | `-10` |
| `pre-open` | `after-hours-fall-threshold` | `-3.0` |
| `pre-open` | `after-hours-fall-penalty` | `-10` |
| `opening` | `vwap-above-score` | `25` |
| `opening` | `near-high-score` | `20` |
| `opening` | `trading-value-score` | `20` |
| `opening` | `vwap-broken-penalty` | `-30` |
| `opening` | `high-drawdown-penalty` | `-20` |
| `opening` | `entry-threshold` | `70` |
| `opening` | `max-candidates` | `3` |
| `follow-up` | `exclude-drawdown-from-high` | `-2.0` |
| `follow-up` | `caution-drawdown-from-high` | `-1.0` |
| `follow-up` | `exclude-when-last-below-vwap` | `true` |
| `follow-up` | `exclude-when-last-below-opening-price` | `true` |
| `follow-up` | `caution-when-previous-high-not-broken` | `true` |
| `follow-up` | `caution-when-previous-high-re-lost` | `true` |
| `price-action` | `previous-high-breakout-score` | `15` |
| `price-action` | `previous-high-not-broken-penalty` | `-10` |
| `price-action` | `opening-price-held-score` | `10` |
| `price-action` | `opening-price-lost-penalty` | `-15` |

설정 예시:

```properties
tradeguard.early-market.strategy.opening.entry-threshold=80
tradeguard.early-market.strategy.opening.max-candidates=2
tradeguard.early-market.strategy.follow-up.exclude-drawdown-from-high=-2.5
tradeguard.early-market.strategy.follow-up.caution-when-previous-high-not-broken=false
```

가산 점수와 상승·거래대금 임계값은 0 이상, 패널티와 하락·낙폭 임계값은 0 이하만 허용합니다. `entry-threshold`는 0~100, `max-candidates`는 1 이상이어야 합니다. 시간외 과열 기준은 상승 기준 이상이어야 하며 follow-up 제외 낙폭은 주의 낙폭 이하, 즉 더 큰 하락이어야 합니다. 위반하면 애플리케이션 시작이 실패합니다.

이 설정은 분석 후보 점수와 follow-up 분류만 조정합니다. 설정 변경으로 자동 주문, 실계좌 주문 또는 시장가 주문이 활성화되지 않습니다.

### 장초반 전략 파라미터 실험 저장

현재 장초반 전략 설정과 기간 리포트 요약을 함께 저장:

```sh
curl -X POST 'http://localhost:8080/api/reports/early-market/experiments' \
  -H 'Content-Type: application/json' \
  -d '{
    "experimentName": "entry threshold 80",
    "from": "2026-06-01",
    "to": "2026-06-10"
  }'
```

저장된 실험 최신순 조회와 단건 조회:

```sh
curl 'http://localhost:8080/api/reports/early-market/experiments?limit=20'
curl 'http://localhost:8080/api/reports/early-market/experiments/1'
```

POST는 기존 기간 리포트와 동일한 최대 90일 검증을 적용합니다. 후보가 한 건 이상인 기간 리포트가 성공한 경우에만 저장하며 후보가 없으면 `404 EARLY_MARKET_STRATEGY_EXPERIMENT_NO_DATA`를 반환합니다. `experimentName`은 필수이고 최대 100자입니다. 최신순 조회 `limit`은 기본 20, 허용 범위는 1~100입니다.

`parameterSnapshot`은 저장 시점의 `preOpen`, `opening`, `followUp`, `priceAction` 설정 전체를 JSON 객체로 보관합니다. 함께 저장되는 결과는 후보 수, 성과 캡처 수, 평균 최대수익률, 평균 최대낙폭, 승률, 최고·최저 후보 signalId입니다. 기간 리포트 후보 상세와 민감정보는 저장하지 않습니다.

실험 저장은 전략 검증 이력 기능입니다. 기간 리포트를 조회하고 요약을 저장할 뿐 모의 주문, 자동 주문, 실계좌 주문 또는 시장가 주문을 생성하지 않습니다.

저장된 장초반 전략 실험 비교:

```sh
curl 'http://localhost:8080/api/reports/early-market/experiments/compare?ids=1,2,3'
```

`ids`는 필수이며 중복 없는 실험 ID를 2개 이상 10개 이하로 전달해야 합니다. 존재하지 않는 ID가 하나라도 있으면 `404 EARLY_MARKET_STRATEGY_EXPERIMENT_NOT_FOUND`를 반환합니다. 응답은 요청 ID 순서의 실험별 저장 결과와 현재 비교 시각, 다음 세 우수 실험을 제공합니다.

- `bestByWinRate`: 저장된 `winRate`가 가장 높은 실험
- `bestByAverageMaxReturnRate`: 저장된 평균 최대수익률이 가장 높은 실험
- `bestByAverageMaxDrawdownRate`: 저장된 평균 최대낙폭이 가장 큰 수치인 실험. 예를 들어 `-0.8%`가 `-2.0%`보다 우수

각 지표가 `null`인 실험은 해당 best 선정 표본에서 제외합니다. 동률이면 요청 ID 목록에서 먼저 나온 실험을 선택합니다. 비교 기간의 `from` 또는 `to`가 하나라도 다르면 notes에 `DIFFERENT_PERIODS`, 최대 후보 수가 최소 후보 수의 2배 이상이면 `DIFFERENT_SAMPLE_SIZE`를 추가합니다.

비교 API는 저장된 실험의 결과와 `parameterSnapshot`만 조회합니다. 기간 리포트를 다시 계산하거나 설정을 변경하지 않으며 모의 주문, 자동 주문, 실계좌 주문 또는 시장가 주문을 실행하지 않습니다.

### 장초반 전략 백테스트 실행

현재 설정을 복사한 임시 설정에 일부 파라미터를 override하고, 저장된 신호·성과 기준 기간 리포트와 실험 저장을 한 번에 수행합니다.

```sh
curl -X POST 'http://localhost:8080/api/reports/early-market/backtests' \
  -H 'Content-Type: application/json' \
  -d '{
    "experimentName": "entry 80 and drawdown -2.5",
    "from": "2026-06-01",
    "to": "2026-06-10",
    "parameterOverrides": {
      "opening": {
        "entryThreshold": 80,
        "maxCandidates": 2
      },
      "followUp": {
        "excludeDrawdownFromHigh": -2.5
      }
    }
  }'
```

`parameterOverrides`와 네 하위 그룹 `preOpen`, `opening`, `followUp`, `priceAction`은 선택 사항입니다. 각 그룹에서도 필요한 필드만 전달하며 null 또는 미지정 필드는 현재 `EarlyMarketStrategyProperties` 값을 사용합니다. 최종 병합 설정은 기존 설정 validation을 모두 통과해야 합니다. 임시 설정은 요청 안에서만 사용되고 전역 Spring Bean이나 이후 scheduler 실행 설정을 변경하지 않습니다.

응답은 저장된 `experiment`, 기간 리포트 핵심 값인 `periodReportSummary`, `warnings`를 제공합니다. 실험의 `parameterSnapshot`에는 override가 적용된 최종 설정 전체가 저장되므로 기존 실험 비교 API의 ID로 바로 사용할 수 있습니다.

현재 백테스트는 과거 원천 시세로 신호를 재생성하는 백테스트가 아닙니다. 이미 저장된 신호, reason, follow-up, 성과를 그대로 집계하므로 다음 경고를 반환합니다.

- `STORED_SIGNALS_NOT_RECALCULATED`: override로 과거 신호 점수나 후보를 다시 생성하지 않음
- `PARAMETER_EFFECT_LIMITED_TO_REPORTING`: 현재 override 효과는 최종 parameter snapshot과 저장 데이터 기반 리포트 기록으로 제한됨
- `MISSING_PERFORMANCE_ROWS`: 기간 후보 중 저장된 성과가 없는 행이 존재함

후보가 없는 기간은 실험을 저장하지 않고 `404 EARLY_MARKET_STRATEGY_EXPERIMENT_NO_DATA`를 반환합니다. 이 API는 KIS 주문, 모의 주문, 자동 주문, 실계좌 주문 또는 시장가 주문을 실행하지 않습니다.

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
- `readiness`: 애플리케이션 readiness, DB, Flyway, KIS read-only 설정, Discord 설정, 14:00/15:00 및 장초반 08:30/09:05/09:20/09:31 scheduler와 시장 calendar bean을 확인한다.
- `marketCalendar`: 현재 연도 DB calendar가 없으면 fallback 사용 가능 상태인 `UNKNOWN`, DB calendar와 향후 30일 내 거래일이 있으면 `UP`, 향후 거래일이 없으면 `DOWN`이다.
- DB는 Spring Boot 기본 DataSource health를 사용한다.
- Flyway pending migration이 있으면 `flywayMigration`이 `DOWN`이다. migration 자체가 실패하면 애플리케이션 시작이 실패하므로 readiness endpoint가 열리지 않는다.
- KIS provider가 `fake`이면 `UP`, `kis`이면서 자격증명이 없으면 `UNKNOWN`, 자격증명이 구성되면 설정 기준 `UP`이다.
- Discord webhook 미설정은 `UNKNOWN`, 설정됨은 `UP`이다.
- KIS/Discord health는 외부 API 호출이나 메시지 전송을 수행하지 않는다.
- API Key, App Secret, Discord webhook URL은 health 응답에 포함하지 않는다.

## Scheduler 실행 이력

14:00 예비 스캔, 15:00 최종 리뷰, 장초반 08:30/09:05 스캔, 09:20 follow-up, 09:31 성과 캡처와 calendar 동기화의 자동 scheduler 실행 이력을 조회할 수 있습니다.

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

응답의 `scannedCount`는 14:00에는 시장 후보군 수, 15:00에는 재검토한 pre-scan 후보 수, 09:20 follow-up에는 확인한 entry candidate 수, 09:31 성과 캡처에는 대상 signal 수를 의미합니다. 09:20의 `selectedCount`는 KEEP 수이고 09:31은 캡처 성공 수입니다. `selectedCount`와 Discord 브리핑 전송 여부인 `notificationSent`도 함께 기록합니다. 수동 scan/review/follow-up/capture API 호출은 scheduler 실행 이력에 포함하지 않습니다.

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
- `tradeguard.early_market.follow_up.count`: `decision=keep|caution|exclude`
- `tradeguard.early_market.follow_up.persist.count`: `result=saved|failed`
- `tradeguard.early_market.price_action.count`: `result=sufficient|insufficient`
- `tradeguard.early_market.report.count`: `result=success|no_data|failure`
- `tradeguard.early_market.period_report.count`: `result=success|no_data|failure`
- `tradeguard.early_market.experiment.count`: `result=saved|no_data|failure`
- `tradeguard.early_market.experiment.compare.count`: `result=success|failure`
- `tradeguard.early_market.backtest.count`: `result=saved|no_data|failure`
- `tradeguard.early_market.performance.capture.count`: `result=bars_used|snapshot_proxy|failed`
- `tradeguard.early_market.data_capture.count`: `captureType`, `result=success|partial|failure`
- `tradeguard.live_order.request.count`: `side`, `status`
- `tradeguard.live_order.submit.count`: `side`, `result=success|failure`
- `tradeguard.live_position.exit_evaluation.count`: `result=hold|take_profit|stop_loss|max_loss|failure`
- `tradeguard.market_calendar.sync.count`: `result=success|fallback|failure`, `year`, `market`
- `tradeguard.market_calendar.lookup.count`: `result=db|fallback|not_found`, `market`
- `tradeguard.market_calendar.override.count`: `result=success|failure`
- `tradeguard.market_calendar.validation.count`: `result=success|failure`

장초반 scheduler는 기존 scheduler metric에 다음 `schedulerName` tag로 기록됩니다.

- `EARLY_MARKET_PRE_OPEN_830`
- `EARLY_MARKET_OPENING_905`
- `EARLY_MARKET_FOLLOW_UP_920`
- `EARLY_MARKET_PERFORMANCE_CAPTURE_930`
- `MARKET_CALENDAR_SYNC`

장초반 scheduler는 평일 Asia/Seoul 기준 08:30, 09:05, 09:20, 09:31에 실행하며 `MarketCalendarPort`가 비거래일로 판단하면 `SKIPPED` 이력을 남깁니다. 거래일에는 `STARTED` 후 `SUCCEEDED` 또는 `FAILED`로 전환하고 후보 또는 캡처 수와 Discord 전송 여부를 저장합니다.

`MARKET_CALENDAR_SYNC`는 매일 Asia/Seoul 04:00에 현재 연도와 다음 연도의 DB 데이터 존재 여부를 확인합니다. 둘 다 있으면 `CALENDAR_YEARS_ALREADY_EXIST`로 skip하고, 누락 연도만 동기화합니다. 실행 이력의 `scannedCount`와 `selectedCount`는 저장 대상으로 처리한 날짜 수이며 `notificationSent=false`입니다. Calendar 동기화는 분석용 기준일 데이터만 갱신하며 자동 주문을 실행하지 않습니다.

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

- 실계좌 주문은 두 feature flag, 계좌 설정, 장중 검사와 kill switch를 통과한 수동 요청 또는 보유 포지션 자동매도에서만 가능합니다.
- 시장가 주문은 지원하지 않습니다.
- 자동매수, 공매도, 신용, 미수 주문은 지원하지 않습니다.
- 08:30/09:05 장초반 후보 생성, 전일 고가/시초가 지지 feature, 09:20 follow-up과 09:31 성과 캡처는 자동 주문을 실행하지 않습니다.
- 09:20 follow-up 결과 저장과 조회는 분석 데이터만 다루며 자동 주문을 실행하지 않습니다.
- 장초반 전략 성과 리포트는 조회와 집계만 수행하며 신호 또는 주문을 생성하지 않습니다.
- 장초반 성과 캡처는 분석 데이터만 저장하며 주문을 생성하지 않습니다.
- 장초반 원천 데이터 아카이브는 replay 입력만 저장하며 주문을 생성하지 않습니다.
- 시간외 데이터 연동은 fake/disabled 또는 설정 기반 KIS read-only 일별 시간외 시세 adapter만 사용합니다.
- KIS 현금 지정가 주문과 현금 체결조회만 사용하며 정정/취소는 아직 구현하지 않습니다.
- API Key, App Secret, 계좌번호는 코드에 하드코딩하지 않습니다.
- Discord Webhook URL은 환경변수로만 주입하며 코드에 하드코딩하지 않습니다.
- 기존 mock order와 `FakeBrokerAdapter` 경로는 그대로 유지됩니다.
