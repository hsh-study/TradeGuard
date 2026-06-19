# TradeGuard

## Operational Dashboard

저장된 운영 데이터만 조회하는 읽기 전용 대시보드 API를 제공한다. 이 API는 KIS/DART 등 외부 provider를 호출하거나 주문을 생성하지 않는다.

```bash
curl http://localhost:8080/api/operations/dashboard
curl 'http://localhost:8080/api/operations/dashboard?baseDate=2026-06-15'
```

응답은 시장 캘린더, Morning Note, 장초/종가 전략, 수급, 실적/DART/valuation,
paper trading, replay backtest, scheduler, KIS token 및 live readiness 상태와 함께
`blockingIssues`, `warnings`, `recommendedActions`를 반환한다.

Spring Boot 기반 한국 주식 자동매매 보조 시스템입니다. MVP 1차는 한국투자증권 Open API 실주문이 아니라 모의투자, 분석, 알림 중심의 구조를 만드는 데 집중합니다.

## MVP 범위

- 관심종목 등록/조회
- 일봉 데이터 및 기술지표 저장 JPA 구조
- MA5, MA20, MA60, RSI(14), MACD, Bollinger Band 계산
- 종가베팅 후보 점수화
- 기본 RiskManager 정책
- FakeBrokerAdapter 기반 모의 주문 요청
- 중복 주문 방지와 신호/주문 요청 이력 저장 구조
- Thesis, Catalyst, Morning Note 기반 1인 투자 하우스형 리서치 워크플로우

## 실행

로컬 운영은 MySQL을 기본으로 사용합니다. 먼저 MySQL에 `tradeguard`
database와 애플리케이션 계정을 만들고 `.env.example`을 참고해 `.env`를
설정합니다.

```sh
set -a
source .env
set +a
./gradlew bootRun
```

환경변수를 지정하지 않으면 `localhost:3306/tradeguard`, 사용자
`tradeguard`를 사용합니다. 실제 비밀번호는 반드시 `.env`의
`DB_PASSWORD`로 설정합니다. `.env`는 Git에 포함되지 않습니다.

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

일반 테스트는 `test` profile과
`src/test/resources/application-test.properties`의 H2 MySQL mode에서 Flyway
migration과 Hibernate schema validation을 검증합니다. Docker가 사용
가능하면 `MySqlMigrationIntegrationTest`가 MySQL Testcontainers로 전체
migration과 핵심 unique constraint도 검증합니다.

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

### Research workflow

Research API는 관심종목과 보유종목에 대한 투자 가설, 무효화 조건,
예정 catalyst와 매일 아침의 수동 점검 항목을 관리합니다. 이 경로는
`BrokerPort`, 실매매 주문 서비스와 연결되지 않으며 thesis가 `BROKEN`이거나
catalyst가 발생해도 자동매수/자동매도를 실행하지 않습니다.

Thesis 등록과 상태 변경:

```sh
curl -X POST 'http://localhost:8080/api/research/theses' \
  -H 'Content-Type: application/json' \
  -d '{
    "stockCode": "005930",
    "title": "HBM 경쟁력 회복",
    "coreAssumption": "고부가 메모리 믹스와 수익성이 개선된다",
    "invalidationCondition": "2개 분기 연속 메모리 마진이 하락한다",
    "targetPrice": 90000,
    "stopLossCondition": "종가가 MA60 아래에서 3거래일 유지된다",
    "confidence": 75,
    "status": "WATCH"
  }'

curl 'http://localhost:8080/api/research/theses?stockCode=005930'

curl -X PATCH 'http://localhost:8080/api/research/theses/1' \
  -H 'Content-Type: application/json' \
  -d '{"confidence":85,"status":"ACTIVE"}'

curl -X POST 'http://localhost:8080/api/research/theses/1/close'
```

Catalyst 등록과 조회:

```sh
curl -X POST 'http://localhost:8080/api/research/catalysts' \
  -H 'Content-Type: application/json' \
  -d '{
    "stockCode": "005930",
    "title": "2분기 실적 발표",
    "catalystType": "EARNINGS",
    "expectedDate": "2026-07-31",
    "importance": "HIGH",
    "status": "UPCOMING",
    "sourceUrl": "https://example.com/disclosure",
    "memo": "메모리 영업이익률과 HBM 출하량 확인"
  }'

curl 'http://localhost:8080/api/research/catalysts?from=2026-07-01&to=2026-08-15'
curl 'http://localhost:8080/api/research/catalysts?stockCode=005930'
```

Morning Note 생성과 조회:

```sh
curl -X POST \
  'http://localhost:8080/api/research/morning-note?tradeDate=2026-06-15'
curl \
  'http://localhost:8080/api/research/morning-note?tradeDate=2026-06-15'
```

Earnings Analysis v1은 뉴스/공시/컨센서스 자동 수집 없이 운영자가 입력한
분기 재무와 valuation snapshot만 사용합니다. 분석 결과는 Morning Note와
종가베팅/장초반 후보 reason에 반영되지만 자동매수/자동매도는 실행하지 않습니다.

```sh
curl -X POST 'http://localhost:8080/api/research/financials/quarterly' \
  -H 'Content-Type: application/json' \
  -d '[{
    "stockCode": "005930",
    "fiscalYear": 2026,
    "fiscalQuarter": 1,
    "revenue": 79000000000000,
    "operatingIncome": 6600000000000,
    "netIncome": 5900000000000,
    "totalAssets": 470000000000000,
    "totalLiabilities": 90000000000000,
    "totalEquity": 380000000000000,
    "operatingCashFlow": 11000000000000,
    "freeCashFlow": 4000000000000
  }]'

curl -X POST 'http://localhost:8080/api/research/valuations' \
  -H 'Content-Type: application/json' \
  -d '{"stockCode":"005930","tradeDate":"2026-06-15","marketCap":500000000000000,"per":12,"pbr":1.2,"psr":1.8}'

curl -X POST \
  'http://localhost:8080/api/research/earnings-analysis?stockCode=005930&baseDate=2026-06-15'

curl -X POST 'http://localhost:8080/api/research/earnings-analysis/batch?baseDate=2026-06-15' \
  -H 'Content-Type: application/json' \
  -d '{"stockCodes":["005930","000660"]}'

curl 'http://localhost:8080/api/research/earnings-analysis?stockCode=005930'
curl 'http://localhost:8080/api/research/earnings-analysis?baseDate=2026-06-15'
```

Valuation Auto Snapshot v1은 운영자가 `valuation_snapshots`를 직접 입력하지
않아도 저장된 일봉 종가, 최신 `quarterly_financials`, 최신 발행주식수
snapshot으로 PER/PBR/PSR을 계산합니다. 자동 생성은 리서치 데이터 보강과
Earnings Analysis 재실행까지만 수행하며 자동매수/자동매도, 실계좌 주문,
시장가 주문과 연결되지 않습니다.

```text
VALUATION_AUTO_SNAPSHOT_ENABLED=true
VALUATION_AUTO_SNAPSHOT_LOOKBACK_DAYS=30
VALUATION_AUTO_SNAPSHOT_REQUIRE_SHARES_OUTSTANDING=true
VALUATION_AUTO_SNAPSHOT_AUTO_ANALYZE=true
```

발행주식수 저장과 조회:

```sh
curl -X POST 'http://localhost:8080/api/research/valuations/shares-outstanding' \
  -H 'Content-Type: application/json' \
  -d '{
    "stockCode": "005930",
    "baseDate": "2026-06-15",
    "sharesOutstanding": 5969782550,
    "source": "MANUAL"
  }'

curl 'http://localhost:8080/api/research/valuations/shares-outstanding?stockCode=005930'
```

Valuation snapshot 생성:

```sh
curl -X POST \
  'http://localhost:8080/api/research/valuations/generate?stockCode=005930&baseDate=2026-06-15'

curl -X POST 'http://localhost:8080/api/research/valuations/generate-batch?baseDate=2026-06-15' \
  -H 'Content-Type: application/json' \
  -d '{"stockCodes":["005930","000660"]}'

curl -X POST \
  'http://localhost:8080/api/research/valuations/generate-watchlist?baseDate=2026-06-15'
```

계산식은 `marketCap = closePrice * sharesOutstanding`,
`eps = netIncome / sharesOutstanding`, `bps = totalEquity / sharesOutstanding`,
`salesPerShare = revenue / sharesOutstanding`입니다. PER은
`marketCap / netIncome`, PBR은 `marketCap / totalEquity`, PSR은
`marketCap / revenue`로 계산합니다. `netIncome <= 0`이면 PER은 `null`이고
`NEGATIVE_EARNINGS` reason을 남깁니다. `totalEquity <= 0`이면 PBR은 `null`,
`revenue <= 0`이면 PSR은 `null`입니다. 일봉 가격이나 발행주식수, 최신 분기
재무가 없으면 `DATA_INSUFFICIENT` 결과를 반환합니다.

`VALUATION_AUTO_SNAPSHOT_AUTO_ANALYZE=true`이면 valuation 생성 성공 후 해당
종목의 Earnings Analysis를 같은 `baseDate`로 재실행합니다. 종가베팅/장초반
후보 생성은 기존 최신 Earnings Analysis를 읽기만 하며, 후보 생성 과정에서
valuation 생성이나 외부 호출을 수행하지 않습니다. `VALUATION_AUTO_SNAPSHOT`
scheduler는 거래일 07:55 Asia/Seoul에 실행되어 전 거래일 기준 관심종목 전체
snapshot 생성을 시도합니다.

Earnings Preview & Post-Earnings Review v1도 운영자 입력 기반입니다. 뉴스,
공시, 컨센서스는 자동 수집하지 않으며 preview/review 결과는 Morning Note와
수동 action item에만 반영됩니다. thesis impact가 `BROKEN`이어도 thesis 상태
변경이나 자동매도는 수행하지 않습니다.

```sh
curl -X POST 'http://localhost:8080/api/research/earnings-events' \
  -H 'Content-Type: application/json' \
  -d '{
    "stockCode": "005930",
    "fiscalYear": 2026,
    "fiscalQuarter": 2,
    "expectedAnnouncementDate": "2026-07-31",
    "memo": "메모리 마진과 HBM 출하 확인"
  }'

curl 'http://localhost:8080/api/research/earnings-events?from=2026-07-01&to=2026-08-10'
curl 'http://localhost:8080/api/research/earnings-events?stockCode=005930'

curl -X PATCH 'http://localhost:8080/api/research/earnings-events/1' \
  -H 'Content-Type: application/json' \
  -d '{"status":"ANNOUNCED","actualAnnouncementDate":"2026-07-31"}'

curl -X POST 'http://localhost:8080/api/research/earnings-previews/generate?stockCode=005930&earningsEventId=1&previewDate=2026-07-25'

curl -X POST 'http://localhost:8080/api/research/earnings-previews' \
  -H 'Content-Type: application/json' \
  -d '{
    "earningsEventId": 1,
    "stockCode": "005930",
    "previewDate": "2026-07-25",
    "keyCheckpoints": ["HBM margin", "server DRAM demand"],
    "expectedRevenue": 79000000000000,
    "expectedOperatingIncome": 6600000000000,
    "expectedNetIncome": 5900000000000,
    "expectedOperatingMargin": 0.0835,
    "expectedRisks": ["FX volatility"],
    "thesisWatchPoints": ["margin recovery thesis"],
    "status": "READY"
  }'

curl 'http://localhost:8080/api/research/earnings-previews?stockCode=005930'
curl 'http://localhost:8080/api/research/earnings-previews/upcoming?from=2026-07-20&to=2026-07-31'

curl -X POST 'http://localhost:8080/api/research/post-earnings-reviews' \
  -H 'Content-Type: application/json' \
  -d '{
    "earningsEventId": 1,
    "stockCode": "005930",
    "reviewDate": "2026-07-31",
    "actualRevenue": 81000000000000,
    "actualOperatingIncome": 7200000000000,
    "actualNetIncome": 6300000000000,
    "thesisImpact": "STRENGTHENED",
    "reviewSummary": "Revenue and operating income beat preview assumptions.",
    "actionItems": ["Update quarterly financials with cash-flow fields"],
    "upsertQuarterlyFinancial": false,
    "rerunEarningsAnalysis": false
  }'

curl 'http://localhost:8080/api/research/post-earnings-reviews?stockCode=005930'
```

Surprise는 `(actual - expected) / abs(expected)`로 계산합니다. 기대값이 없거나
0이면 surprise는 `null`입니다. `upsertQuarterlyFinancial=true`를 주더라도
post review 입력만으로 자산/부채/자본/현금흐름 필드가 부족하므로 자동 upsert는
하지 않고 `QUARTERLY_FINANCIAL_UPSERT_REQUIRED` action item을 생성합니다.

Morning Note에는 `UPCOMING_EARNINGS`, `EARNINGS_PREVIEW_READY`,
`EARNINGS_REVIEW_REQUIRED`, `POST_EARNINGS_WEAKENED/BROKEN` 항목이 추가됩니다.
earnings event 생성 시 `EARNINGS_EVENT_AUTO_CREATE_CATALYST=true`이면 같은
종목·발표일·분기의 `EARNINGS` catalyst를 중복 없이 자동 생성합니다.

Catalyst Evidence v1은 catalyst와 thesis 판단에 붙일 공식 공시 기반 근거를
저장합니다. 뉴스 크롤링을 하지 않으며, 공시 원문 전체를 DB에 저장하지 않고
제목, 요약, source name, source URL, 게시 시각 같은 메타데이터와 링크 중심으로
관리합니다. earnings event, READY preview, post earnings review는 각각
`EARNINGS_EVENT`, `EARNINGS_PREVIEW`, `POST_EARNINGS_REVIEW` evidence를
중복 없이 자동 생성합니다. 자동 주문은 실행하지 않습니다.

```sh
curl -X POST 'http://localhost:8080/api/research/evidences' \
  -H 'Content-Type: application/json' \
  -d '{
    "catalystId": 1,
    "stockCode": "005930",
    "evidenceType": "DART_DISCLOSURE",
    "title": "단일판매ㆍ공급계약체결",
    "summary": "공식 DART 공시 메타데이터 기반 요약",
    "sourceName": "DART",
    "sourceUrl": "https://dart.fss.or.kr/...",
    "sourcePublishedAt": "2026-06-15T00:00:00Z",
    "confidence": "HIGH"
  }'

curl 'http://localhost:8080/api/research/catalysts/1/evidences'
curl 'http://localhost:8080/api/research/evidences?stockCode=005930'

curl -X PATCH 'http://localhost:8080/api/research/evidences/1' \
  -H 'Content-Type: application/json' \
  -d '{"summary":"요약 보강","confidence":"MEDIUM"}'

curl -X DELETE 'http://localhost:8080/api/research/evidences/1'
```

동일 `stockCode`, `title`, `sourcePublishedAt`, `sourceName` 조합은 중복
evidence로 보고 기존 ACTIVE evidence를 재사용합니다. `catalystId` 없이
evidence를 먼저 저장할 수 있고, 이후 같은 종목의 catalyst와 수동 연결할 수
있습니다. HIGH importance catalyst에 evidence가 없으면 Morning Note에
`HIGH_IMPORTANCE_CATALYST_WITHOUT_EVIDENCE`가 표시됩니다.

Disclosure Evidence Provider v1은 기본 disabled adapter로 구성되어 있으며,
추후 DART/KRX 공시 목록 API 또는 합법 provider를 연결할 수 있는 port를
분리했습니다. provider가 비활성일 때 import API는 `SKIPPED` history를 남깁니다.
API key, source URL, raw response 전체는 로그/응답/metric tag에 남기지 않습니다.

```text
DISCLOSURE_PROVIDER_ENABLED=false
DISCLOSURE_PROVIDER_TYPE=DART
DISCLOSURE_PROVIDER_TIMEOUT_SECONDS=10
```

```sh
curl -X POST \
  'http://localhost:8080/api/research/disclosures/evidences/import?stockCode=005930&from=2026-06-01&to=2026-06-30'

curl 'http://localhost:8080/api/research/disclosures/evidences/import-histories'
```

DART Financial Import v1은 운영자가 `quarterly_financials`를 매번 수동
입력하지 않아도 되도록 공식 OpenDART 재무제표 API에서 분기 재무를 가져오는
구조입니다. 뉴스 크롤링, 공시 원문 저장, 컨센서스 무단 수집은 하지 않습니다.
기본값은 `DART_PROVIDER_ENABLED=false`라 외부 호출이 비활성입니다.
`DART_API_KEY`는 로그, API 응답, health, metric tag에 포함하지 않습니다.

```text
DART_PROVIDER_ENABLED=false
DART_API_BASE_URL=
DART_API_KEY=
DART_REQUEST_TIMEOUT_SECONDS=10
DART_IMPORT_AUTO_ANALYZE=true
DART_IMPORT_LOOKBACK_QUARTERS=8
DART_CORP_CODE_IMPORT_ENABLED=false
DART_CORP_CODE_ZIP_URL=
DART_CORP_CODE_IMPORT_TIMEOUT_SECONDS=20
DART_CORP_CODE_IMPORT_AUTO_MATCH_LISTED_ONLY=true
```

Corp mapping 등록과 조회:

```sh
curl -X POST 'http://localhost:8080/api/research/dart/corp-mappings' \
  -H 'Content-Type: application/json' \
  -d '{
    "stockCode": "005930",
    "corpCode": "00126380",
    "corpName": "삼성전자",
    "market": "KOSPI"
  }'

curl 'http://localhost:8080/api/research/dart/corp-mappings?stockCode=005930'
curl 'http://localhost:8080/api/research/dart/corp-mappings'
```

DART corp code import는 공식 OpenDART corpCode zip 또는 합법 provider가
제공하는 동일 XML 파일을 메모리에서 파싱해 `dart_corp_mappings`를 upsert하는
구조입니다. 기본값은 `DART_CORP_CODE_IMPORT_ENABLED=false`라 외부 호출이
비활성입니다. 원본 zip/xml 전체를 DB에 저장하지 않으며, `DART_CORP_CODE_ZIP_URL`
또는 API key가 로그, 응답, metric tag에 노출되지 않도록 실패 사유를 정리합니다.

```sh
curl -X POST 'http://localhost:8080/api/research/dart/corp-codes/import'
curl 'http://localhost:8080/api/research/dart/corp-codes/import-histories'
```

corpCode XML의 `corp_code`, `corp_name`, `stock_code`, `modify_date`를
파싱합니다. `stock_code`가 비어 있으면 기본 정책에서 비상장/비매핑 항목으로
보고 skip합니다. `stock_code`가 있으면 `dart_corp_mappings`를 upsert하며,
기존 mapping에 `KOSPI`, `KOSDAQ`, `KONEX` market 값이 있으면 보존합니다.
신규 자동 mapping의 market은 `UNKNOWN`으로 저장합니다.

DART 재무제표 import:

```sh
curl -X POST \
  'http://localhost:8080/api/research/dart/financials/import?stockCode=005930&fiscalYear=2026&reportCode=11013'

curl -X POST \
  'http://localhost:8080/api/research/dart/financials/import-recent?stockCode=005930&baseDate=2026-06-15'

curl -X POST \
  'http://localhost:8080/api/research/dart/financials/import-watchlist?baseDate=2026-06-15'

curl \
  'http://localhost:8080/api/research/dart/financials/import-histories?stockCode=005930'
```

보고서 코드는 `11013 -> Q1`, `11012 -> Q2`, `11014 -> Q3`,
`11011 -> Q4`로 매핑합니다. 계정명은 exact matching을 우선하고 공백과
괄호를 제거한 normalized fallback을 사용합니다. 매출액, 영업이익,
당기순이익, 자산총계, 부채총계, 자본총계, 영업활동현금흐름을 찾으면
`quarterly_financials`에 upsert하며, OpenDART가 직접 제공하지 않는
`freeCashFlow`는 `null`로 저장합니다.

`DART_IMPORT_AUTO_ANALYZE=true`이면 import가 `SUCCESS`로 재무 데이터를 저장한
뒤 해당 종목의 Earnings Analysis를 재실행합니다. `PARTIAL`, `FAILED`,
`SKIPPED` 이력은 저장하지만 자동 분석은 실행하지 않습니다. 이 흐름은
자동매수, 자동매도, thesis 상태 변경, 실계좌 주문과 연결되지 않습니다.
Morning Note에는 `DART_MAPPING_REQUIRED`, `DART_IMPORT_REQUIRED`,
`DART_IMPORT_FAILED`, `DART_IMPORT_RECENT_EARNINGS_STATUS` action item이
추가됩니다.

Valuation Auto Snapshot 관련해서는 Morning Note에
`VALUATION_DATA_INSUFFICIENT`, `SHARES_OUTSTANDING_REQUIRED`,
`VALUATION_AUTO_GENERATED`, `VALUATION_NEGATIVE_EARNINGS`,
`VALUATION_OVERVALUED_WARNING` action item이 추가됩니다.

발행주식수 import v1은 DART 재무제표 API만으로 안정적으로 수집하기 어려운
필드이므로 CSV 입력을 제공합니다. 추후 DART 사업보고서의 주식의 총수 parsing
provider 또는 합법 provider를 붙일 수 있도록 port만 분리했습니다.

```text
SHARES_OUTSTANDING_IMPORT_AUTO_GENERATE_VALUATION=false
```

CSV columns는 `stockCode,baseDate,sharesOutstanding,source`입니다.

```sh
curl -X POST 'http://localhost:8080/api/research/valuations/shares-outstanding/import-csv' \
  -H 'Content-Type: text/csv' \
  --data-binary $'stockCode,baseDate,sharesOutstanding,source\n005930,2026-06-15,5969782550,MANUAL\n'

curl 'http://localhost:8080/api/research/valuations/shares-outstanding/import-histories'
```

`SHARES_OUTSTANDING_IMPORT_AUTO_GENERATE_VALUATION=true`이면 CSV로 저장된 각
행에 대해 같은 `baseDate`로 valuation generate를 선택적으로 실행합니다. 이
옵션은 Earnings Analysis 자동 재실행 옵션과 별개이며, 자동 주문과 연결되지
않습니다. Morning Note에는 `DART_CORP_MAPPING_IMPORTED`,
`DART_CORP_MAPPING_IMPORT_FAILED`, `SHARES_OUTSTANDING_IMPORT_REQUIRED`,
`SHARES_OUTSTANDING_IMPORT_FAILED`, `SHARES_OUTSTANDING_IMPORTED`가 추가됩니다.

Evidence 관련해서는 Morning Note에 `NEW_DISCLOSURE_EVIDENCE`,
`HIGH_CONFIDENCE_CATALYST_EVIDENCE`,
`HIGH_IMPORTANCE_CATALYST_WITHOUT_EVIDENCE`,
`POST_EARNINGS_REVIEW_EVIDENCE`, `DISCLOSURE_IMPORT_FAILED`가 추가됩니다.

Market Index Provider & Import v1:

```sh
curl -X POST 'http://localhost:8080/api/research/market-indices' \
  -H 'Content-Type: application/json' \
  -d '{
    "indexCode": "KOSPI",
    "indexName": "KOSPI",
    "tradeDate": "2026-06-12",
    "closePrice": 2800.0000,
    "changeRate": 1.2500,
    "tradingValue": 9000000000000
  }'

curl 'http://localhost:8080/api/research/market-indices?tradeDate=2026-06-12'
```

Market index CSV import columns는
`indexCode,indexName,tradeDate,closePrice,changeRate,tradingValue`입니다.

```sh
curl -X POST 'http://localhost:8080/api/research/market-indices/import-csv' \
  -H 'Content-Type: text/csv' \
  --data-binary $'indexCode,indexName,tradeDate,closePrice,changeRate,tradingValue\nKOSPI,KOSPI,2026-06-12,2800,1.25,9000000000000\nKOSDAQ,KOSDAQ,2026-06-12,900,-0.5,3000000000000\n'

curl -X POST \
  'http://localhost:8080/api/research/market-indices/import?tradeDate=2026-06-12'

curl 'http://localhost:8080/api/research/market-indices/import-histories'
```

`MARKET_INDEX_PROVIDER_ENABLED=false`가 기본값이므로 provider import는 외부
KIS 호출 없이 `SKIPPED` 이력을 남깁니다. `MARKET_INDEX_IMPORT_AUTO_RUN=true`
이고 provider가 enabled일 때만 거래일 07:50 Asia/Seoul scheduler가 전
거래일 major index import를 시도합니다. KIS credential, token, 응답 본문은
로그나 metric tag에 노출하지 않으며, 시장지수 수집 실패는 자동 주문 흐름과
연결되지 않습니다.

Sector master와 snapshot:

```sh
curl -X POST 'http://localhost:8080/api/research/sectors' \
  -H 'Content-Type: application/json' \
  -d '{
    "sectorCode": "SEMICONDUCTOR",
    "sectorName": "반도체",
    "sectorType": "THEME"
  }'

curl -X POST 'http://localhost:8080/api/research/sectors/SEMICONDUCTOR/stocks' \
  -H 'Content-Type: application/json' \
  -d '{"stockCode":"005930","source":"MANUAL"}'

curl 'http://localhost:8080/api/research/sectors'
curl -X POST \
  'http://localhost:8080/api/research/sectors/snapshots?tradeDate=2026-06-12'
curl \
  'http://localhost:8080/api/research/sectors/SEMICONDUCTOR/snapshot?tradeDate=2026-06-12'
```

Sector seed CSV import columns는
`sectorCode,sectorName,sectorType,stockCode,source`입니다. `stockCode`가
비어 있으면 sector master만 upsert하고, 값이 있으면 stock-sector mapping도
중복 없이 upsert합니다. `sectorType`이 비어 있으면 `CUSTOM`, `source`가 비어
있으면 `CSV`를 사용합니다.

```sh
curl -X POST 'http://localhost:8080/api/research/sectors/import-csv' \
  -H 'Content-Type: text/csv' \
  --data-binary $'sectorCode,sectorName,sectorType,stockCode,source\nSEMICONDUCTOR,반도체,THEME,005930,CSV\nSEMICONDUCTOR,반도체,THEME,000660,CSV\nBIO,바이오,CUSTOM,,CSV\n'

curl 'http://localhost:8080/api/research/sectors/import-histories'
```

`SECTOR_IMPORT_AUTO_GENERATE_SNAPSHOT=true`이면 sector CSV import 성공 후
전 거래일 기준 sector snapshot 생성을 선택적으로 실행합니다. 이 옵션도
리서치 데이터 보강 전용이며 자동매수/자동매도, 실계좌 주문, 시장가 주문과
연결되지 않습니다.

Investor Flow / Supply-Demand Auto Import v1:

```text
INVESTOR_FLOW_PROVIDER_ENABLED=false
INVESTOR_FLOW_PROVIDER_TYPE=KIS
INVESTOR_FLOW_PROVIDER_TIMEOUT_SECONDS=10
INVESTOR_FLOW_IMPORT_AUTO_RUN=false
INVESTOR_FLOW_LOOKBACK_DAYS=20
KIS_INVESTOR_FLOW_AMOUNT_UNIT=UNVERIFIED
KIS_INVESTOR_FLOW_DIAGNOSTIC_ENABLED=false
KIS_INVESTOR_FLOW_DIAGNOSTIC_ALLOW_HTTP=false
KIS_INVESTOR_FLOW_DIAGNOSTIC_MASK_RESPONSE=true
SUPPLY_DEMAND_STRATEGY_ENABLED=true
```

provider import가 핵심 경로이며, 07:40 `INVESTOR_FLOW_IMPORT`가 전 거래일의
활성 관심종목 수급을 저장하고 07:45 `SUPPLY_DEMAND_ANALYSIS`가 snapshot을
생성합니다. KIS read-only adapter는 공식 KIS Open Trading API 샘플에서 확인한
종목별 투자자매매동향 TR `FHKST01010900`
(`/uapi/domestic-stock/v1/quotations/inquire-investor`)과 시장별 투자자매매동향
TR `FHPTJ04040000`
(`/uapi/domestic-stock/v1/quotations/inquire-investor-daily-by-market`)을 사용합니다.
종목 TR은 실전/모의 환경에서 사용할 수 있고, 공식 샘플이 실전 전용으로 표시한
시장 TR은 `KIS_ENVIRONMENT=REAL`에서만 호출합니다. 시장 adapter v1은 공식
파라미터가 확인된 KOSPI(`KSP`, `0001`)와 KOSDAQ(`KSQ`, `1001`)만 지원합니다.

공식 응답 mapping은 거래일 `stck_bsop_date`, 순매수 수량
`prsn/frgn/orgn_ntby_qty`, 순매수 금액 `prsn/frgn/orgn_ntby_tr_pbmn`, 매수 수량
`*_shnu_vol`, 매도 수량 `*_seln_vol`, 매수 금액 `*_shnu_tr_pbmn`, 매도 금액
`*_seln_tr_pbmn`입니다. `prsn`, `frgn`, `orgn`은 각각 `INDIVIDUAL`, `FOREIGN`,
`INSTITUTION`으로 저장하고 raw 분류명도 보존합니다. 시장 TR이 제공하지 않는
매수/매도 분리 필드는 null로 저장합니다.

공식 샘플 코드에는 `*_tr_pbmn`의 금액 단위가 명시되어 있지 않으므로 추측하지
않습니다. 기본 `KIS_INVESTOR_FLOW_AMOUNT_UNIT=UNVERIFIED`에서는 token/HTTP 호출
전에 `Unsupported`로 실패합니다. 운영에서 실응답 단위를 확인한 뒤에만 `KRW`,
`THOUSAND_KRW`, `MILLION_KRW` 중 하나를 설정하며 DB에는 모두 원화로 환산해
저장합니다. 수량 필드는 `SHARE`, multiplier 1로 원본 값을 그대로 저장합니다.
provider가 꺼져 있으면 API와 scheduler는 외부 호출 없이 `SKIPPED` history를
남깁니다. CSV는 provider 장애 또는 초기 적재를 위한 fallback이며 후보 생성은
provider를 호출하지 않고 저장된 snapshot만 읽습니다.

KIS Investor Flow Verification v1은 금액 단위를 추측하지 않고 운영 실응답을
확인하기 위한 diagnostic 전용 경로입니다. diagnostic은 `stock_investor_flows`,
`market_investor_flows`, import history 또는 supply-demand snapshot을 저장하지
않으며 scheduler와 후보 생성에서도 호출하지 않습니다. 원문 응답 전체, HTTP
header, token, app key/secret, 계좌번호는 반환하거나 로그에 남기지 않습니다.
기본 마스킹은 허용된 숫자 필드를 `POSITIVE_DIGITS_3` 같은 부호/자릿수 정보로만
반환합니다.

```sh
curl -X POST \
  'http://localhost:8080/api/research/investor-flows/verify/stock?stockCode=005930&tradeDate=2026-06-15'

curl -X POST \
  'http://localhost:8080/api/research/investor-flows/verify/market?market=KOSPI&tradeDate=2026-06-15'
```

운영 검증 절차:

1. DART/KIS credential과 기존 read-only 설정을 완료합니다.
2. `INVESTOR_FLOW_PROVIDER_ENABLED=true`로 설정합니다.
3. `KIS_INVESTOR_FLOW_AMOUNT_UNIT=UNVERIFIED`를 유지합니다.
4. `KIS_INVESTOR_FLOW_DIAGNOSTIC_ENABLED=true`로 설정합니다.
5. `KIS_INVESTOR_FLOW_DIAGNOSTIC_ALLOW_HTTP=true`로 설정합니다.
6. verify API로 삼성전자 등 표본 종목과 거래일을 조회합니다.
7. KIS HTS/앱 또는 공식 응답 문서와 금액 단위를 비교합니다. 정확한 whitelist
   숫자 비교가 필요한 통제된 로컬 환경에서만
   `KIS_INVESTOR_FLOW_DIAGNOSTIC_MASK_RESPONSE=false`를 잠시 사용합니다.
8. 확인된 단위에 따라 `KRW`, `THOUSAND_KRW`, `MILLION_KRW` 중 하나를 설정합니다.
9. diagnostic enabled/allow HTTP를 다시 `false`로 변경합니다.
10. 일반 import API를 실행합니다.
11. 생성된 supply-demand snapshot을 확인합니다.

diagnostic 또는 HTTP 허용 설정이 꺼져 있으면 verify API는 안전한 409 응답을
반환합니다. KIS provider가 활성화된 상태에서 금액 단위가 `UNVERIFIED`이면 일반
종목/시장/watchlist import는 provider나 DB를 호출하지 않고
`SKIPPED / AMOUNT_UNIT_UNVERIFIED` history를 남깁니다. 07:40 import와 07:45
supply-demand scheduler도 같은 reason으로 skip하며, 수동 supply-demand 분석은
snapshot 저장 전에 차단됩니다. 검증 중 우회 저장을 막기 위해 KIS provider가
활성화된 이 상태에서는 CSV fallback도 flow row를 저장하지 않습니다. provider를
비활성화한 순수 CSV fallback 운영은 기존 정책대로 사용할 수 있습니다.

Investor Flow Operational Readiness v1은 외부 KIS 호출 없이 현재 설정과 최근
종목/시장 import history, `SUPPLY_DEMAND_ANALYSIS` scheduler 이력을 점검합니다.

```sh
curl 'http://localhost:8080/api/research/investor-flows/readiness'
```

```json
{
  "providerEnabled": true,
  "providerType": "KIS",
  "amountUnit": "UNVERIFIED",
  "amountUnitVerified": false,
  "diagnosticEnabled": true,
  "diagnosticAllowHttp": true,
  "diagnosticMaskResponse": true,
  "importAutoRun": false,
  "lookbackDays": 20,
  "latestStockImportStatus": null,
  "latestMarketImportStatus": null,
  "latestSupplyDemandAnalysisStatus": null,
  "ready": false,
  "blockingReasons": ["AMOUNT_UNIT_UNVERIFIED"],
  "warnings": ["DIAGNOSTIC_MODE_ENABLED", "DIAGNOSTIC_HTTP_ENABLED"],
  "recommendedNextActions": [
    "Run verify stock API",
    "Compare KIS HTS amount unit",
    "Set KIS_INVESTOR_FLOW_AMOUNT_UNIT"
  ]
}
```

운영 활성화 전에는 verify API로 금액 단위를 확인하고, 확인된 값으로
`KIS_INVESTOR_FLOW_AMOUNT_UNIT`을 설정한 뒤 diagnostic enabled/allow HTTP를
모두 `false`로 되돌립니다. 그 다음 일반 import를 실행해 종목/시장 최근 상태가
성공인지 확인하고 `INVESTOR_FLOW_IMPORT_AUTO_RUN=true`를 적용합니다. provider가
비활성화된 경우 readiness는 수동/CSV fallback을 허용하기 위해 `ready=true`와
warning을 반환합니다. Actuator `investorFlowProvider` health는 provider disabled
또는 검증 완료 시 `UP`, auto-run이 켜진 미검증 상태에서는 `OUT_OF_SERVICE`이며
secret, token, header, 계좌정보를 details에 포함하지 않습니다.

```sh
curl -X POST \
  'http://localhost:8080/api/research/investor-flows/stocks/import?stockCode=005930&tradeDate=2026-06-15'
curl -X POST \
  'http://localhost:8080/api/research/investor-flows/markets/import?market=KOSPI&tradeDate=2026-06-15'
curl -X POST \
  'http://localhost:8080/api/research/investor-flows/watchlist/import?tradeDate=2026-06-15'
curl 'http://localhost:8080/api/research/investor-flows/import-histories?stockCode=005930'
curl 'http://localhost:8080/api/research/investor-flows/stocks/recent?stockCode=005930&endDate=2026-06-15&days=20'
```

CSV는 provider 장애나 초기 적재를 위한 fallback입니다. 종목 CSV columns는
`stockCode,tradeDate,investorType,netBuyAmount,netBuyQuantity,buyAmount,sellAmount,buyQuantity,sellQuantity,source`,
시장 CSV columns는
`market,tradeDate,investorType,netBuyAmount,netBuyQuantity,buyAmount,sellAmount,source`입니다.

```sh
curl -X POST 'http://localhost:8080/api/research/investor-flows/stocks/import-csv' \
  -H 'Content-Type: text/csv' \
  --data-binary $'stockCode,tradeDate,investorType,netBuyAmount,netBuyQuantity,buyAmount,sellAmount,buyQuantity,sellQuantity,source\n005930,2026-06-15,FOREIGN,1000000000,10000,3000000000,2000000000,30000,20000,CSV\n'

curl -X POST \
  'http://localhost:8080/api/research/supply-demand/analyze?stockCode=005930&tradeDate=2026-06-15'
curl -X POST \
  'http://localhost:8080/api/research/supply-demand/analyze-watchlist?tradeDate=2026-06-15'
curl 'http://localhost:8080/api/research/supply-demand?stockCode=005930'
```

smart money는 외국인과 기관 합계입니다. 최근 데이터가 3일 미만이면
`DATA_INSUFFICIENT`, 점수 50 이상은 `STRONG_ACCUMULATION`, 20 이상은
`NEUTRAL`, 20 미만은 `DISTRIBUTION`입니다. 종베와 장초 후보는 기본적으로
강한 매집에 +10, 분산에 -10을 적용하고 부족 상태는 reason만 추가합니다.
개인 순매수가 크면서 외국인과 기관이 함께 순매도하면 caution reason도
저장됩니다. KIS adapter와 이 분석은 계좌번호를 사용하지 않는 read-only 흐름이며
자동매수/자동매도, 실계좌 주문 또는 시장가 주문을 생성하지 않습니다.

Sector snapshot은 섹터에 매핑된 종목의 기준일 종가와 직전 거래일 종가로
등락률을 계산하고 평균, 중앙값, 총 거래대금, 상승/하락 종목 수, 가장 강한
leading stock을 저장합니다. 계산 가능한 종목이 없으면 스냅샷은
`DATA_INSUFFICIENT` 상태로 Morning Note에 노출됩니다. 자동 주문은 실행하지
않습니다.

`RESEARCH_MORNING_NOTE` scheduler는 거래일 08:10 Asia/Seoul에 실행됩니다.
`SECTOR_DAILY_SNAPSHOT` scheduler는 거래일 08:05 Asia/Seoul에 먼저 실행되어
전 거래일 기준 섹터 snapshot을 생성합니다.
Discord 전송은 기본 비활성이며 아래 설정으로만 활성화됩니다.

```text
RESEARCH_MORNING_NOTE_DISCORD_ENABLED=true
DISCORD_WEBHOOK_URL=...
```

Morning Note 예시:

```text
marketSummary:
전 거래일 2026-06-12 저장 후보 2건
시장 지수
- KOSPI(KOSPI) close=2800.0000 changeRate=1.2500% tradingValue=9000000000000.0000
- CLOSING_BET 005930 score=80 status=CREATED
- EARLY_MARKET 000660 score=74 status=RISK_APPROVED

sectorSummary:
섹터 가격/거래대금 요약 기준일 2026-06-12
상위 섹터
- 반도체(SEMICONDUCTOR) avg=2.5000% median=2.3000% value=90000000000.0000 up/down=2/1 leader=005930(4.1000%)
하위 섹터
- 바이오(BIO) avg=-1.1000% median=-0.8000% value=30000000000.0000 up/down=1/2 leader=068270(1.0000%)

watchlistSummary:
활성 관심종목 1개
- 005930 삼성전자 close=71000 vsMA20=ABOVE vsMA60=ABOVE
  ma20>ma60=true RSI=NEUTRAL(58.2) MACD=BULLISH Bollinger=INSIDE sectors=반도체(SEMICONDUCTOR)

actionItems:
자동 주문 없음. 수동 리서치 체크리스트
- UPCOMING_CATALYST 2026-06-19 005930 [HIGH] 실적 사전 전망
- BROKEN_THESIS 000660 수요 회복: 고객사 재고가 재상승
- DATA_INSUFFICIENT 035420 일봉/지표 보강 확인
- MARKET_INDEX_DATA_UNAVAILABLE 2026-06-12 시장지수 데이터 없음
- MARKET_INDEX_IMPORT_FAILED KIS provider failed
- SECTOR_IMPORT_REQUIRED sector master CSV import 필요
- SECTOR_MAPPING_INSUFFICIENT unmappedStocks=3
- SECTOR_IMPORT_FAILED invalidRows=2
- SUPPLY_DEMAND_STRONG 005930 smartMoneyDays=3 smartMoney=1200000000
- SUPPLY_DEMAND_DISTRIBUTION 034220 foreign=-500000000 institution=-300000000
- INVESTOR_FLOW_PROVIDER_DISABLED provider disabled 상태
- INVESTOR_FLOW_IMPORT_REQUIRED 관심종목 수급 자동 import 필요 missing=2
- INVESTOR_FLOW_NOT_READY provider=KIS
- AMOUNT_UNIT_UNVERIFIED verify KIS amount unit and configure KIS_INVESTOR_FLOW_AMOUNT_UNIT
```

뉴스와 실적 원문 수집은 이번 범위에 포함되지 않습니다. 시장지수는
`market_indices` 저장 데이터가 있을 때 Morning Note에 반영되며, 섹터
snapshot이 없으면 `SECTOR_DATA_UNAVAILABLE`, 섹터 구성 종목의 일봉이
부족하면 `DATA_INSUFFICIENT`를 명시합니다. Market/Sector import 관련
Morning Note action item은 `MARKET_INDEX_IMPORT_FAILED`,
`MARKET_INDEX_DATA_UNAVAILABLE`, `SECTOR_IMPORT_REQUIRED`,
`SECTOR_IMPORT_FAILED`, `SECTOR_MAPPING_INSUFFICIENT`입니다.

### KIS 수동 승인형 실매매

Live trading은 기본 비활성입니다. 신규 주문은 아래 조건을 모두 만족할 때만 허용됩니다.

- `LIVE_TRADING_ENABLED=true`
- `KIS_TRADING_ENABLED=true`
- 계좌번호와 계좌상품코드 설정
- DB kill switch 비활성
- `MarketCalendarPort` 기준 거래일의 09:00~15:30
- `LIMIT` 지정가 주문
- 주문금액이 `LIVE_MAX_ALLOWED_ORDER_AMOUNT` 이하

운영 종합 점검:

```sh
curl 'http://localhost:8080/api/live-trading/readiness'
```

응답은 feature flag, REAL/DEMO 환경, 계좌 설정 여부, token 만료 상태,
kill switch, DB calendar, 현재 장 운영시간, 지정가/주문한도,
세금·수수료와 자동취소 정책을 한 번에 확인합니다. 하나 이상의
`blockingReasons`가 있으면 `ready=false`입니다. 장 종료와 자동취소
비활성은 `warnings`로 반환됩니다. 계좌번호, app key/secret, access token
원문은 응답에 포함되지 않습니다.

실매매 배포 체크리스트와 credential rotation 절차는
[`docs/live-trading-operations.md`](docs/live-trading-operations.md)를
따릅니다.

실전은 `KIS_TRADING_ENVIRONMENT=REAL`, 모의는 `DEMO`로 선택합니다. 환경에 따라 실전 `https://openapi.koreainvestment.com:9443` 또는 모의 `https://openapivts.koreainvestment.com:29443`가 자동 선택되며 운영 설정에서 별도 주문 base URL을 받지 않습니다. KIS 공식 현금주문 endpoint `/uapi/domestic-stock/v1/trading/order-cash`를 사용하며 실전 매수/매도 TR_ID는 `TTTC0012U`/`TTTC0011U`, 모의는 `VTTC0012U`/`VTTC0011U`입니다. KRX 직접 주문 API는 사용하지 않습니다.

### KIS OAuth tokenP

read-only 시세 환경은 `KIS_READ_ONLY_ENVIRONMENT=DEMO`, live 주문 환경은 `KIS_TRADING_ENVIRONMENT=REAL|DEMO`로 독립 지정합니다. 두 환경의 access token은 절대 공유하지 않고 환경별 MEMORY cache에 저장합니다.

- token 없음: `/oauth2/tokenP` 1회 발급
- 유효 token: cache hit
- 만료 600초 전: 갱신
- `KIS_TOKEN_DAILY_REFRESH_ENABLED=true`: KST 발급일이 바뀌면 갱신
- 갱신 실패: 기존 token이 아직 유효하면 유지, 만료됐으면 요청 실패
- 동시 요청: 환경별 lock으로 tokenP 중복 호출 방지
- `KIS_TOKEN_REFRESH`: 매일 `KIS_TOKEN_ISSUE_TIME_KST` 기본 07:30 KST에 사용 중인 환경만 갱신

```sh
curl 'http://localhost:8080/api/kis/token/status'
curl -X POST \
  'http://localhost:8080/api/kis/token/refresh?environment=DEMO'
```

응답에는 `environment`, `tokenPresent`, `expiresAt`, `secondsToExpire`, `dailyIssuedDate`만 포함되며 access token 원문은 반환하지 않습니다. `kisOAuthToken` health도 동일하게 만료 정보만 노출합니다.

Token cache mode:

- `KIS_TOKEN_CACHE_MODE=MEMORY`: 기본값. 인스턴스별 메모리에 token을 저장하며 재시작 시 재발급합니다.
- `KIS_TOKEN_CACHE_MODE=DB`: REAL/DEMO별 암호화 token을 `kis_access_tokens`에 저장해 로컬 애플리케이션 재시작 후에도 유효한 token을 재사용합니다. DB lease lock은 scheduler와 요청이 겹칠 때 중복 tokenP 발급을 막습니다.

DB 모드는 Base64로 인코딩된 32바이트 AES key가 반드시 필요합니다.

```sh
openssl rand -base64 32
```

```text
KIS_TOKEN_CACHE_MODE=DB
KIS_TOKEN_ENCRYPTION_KEY=<base64-encoded-32-byte-key>
KIS_TOKEN_REFRESH_LOCK_TIMEOUT_SECONDS=120
KIS_TOKEN_REFRESH_LOCK_WAIT_SECONDS=10
```

Token은 AES-256-GCM으로 암호화되고 nonce와 authentication tag가 ciphertext에 포함됩니다. 평문 token은 DB, API, health, log, metric에 저장하거나 노출하지 않습니다. 동일 환경 refresh lock이 이미 점유 중이면 유효한 기존 token을 재사용합니다. 비정상 종료로 남은 오래된 lock은 timeout 이후 회수할 수 있습니다.

```sh
curl 'http://localhost:8080/api/kis/token/status'
curl -X DELETE \
  'http://localhost:8080/api/kis/token?environment=REAL'
```

Status 응답에는 `cacheMode`와 `refreshInProgress`가 추가되며 token 원문은 포함되지 않습니다. `KIS_BASE_URL_OVERRIDE`는 테스트 전용이며 운영에서는 비워 둡니다.

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

`LIVE_ORDER_RECONCILIATION`은 `KIS_TRADING_ENABLED=true`일 때 평일 1분마다 `ACCEPTED`/`PARTIALLY_FILLED` 주문의 누적 체결량과 잔량을 조회합니다. 새로 늘어난 체결 수량만 fill로 저장하고 매수 평균가/수량 또는 매도 잔여 포지션에 반영합니다. 전량 체결은 `FILLED`, 일부 체결은 `PARTIALLY_FILLED`로 전환됩니다. 주문·취소·체결조회 전에 공통 token provider에서 해당 live 환경 token을 획득하며 token 발급 또는 만료 복구에 실패하면 실주문 요청은 차단됩니다.

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

관심종목 등록 시 종목 저장을 먼저 완료한 뒤 KIS 읽기 전용 일봉 warmup을
실행합니다.

```sh
curl -X POST 'http://localhost:8080/api/stocks' \
  -H 'Content-Type: application/json' \
  -d '{"stockCode":"005930","stockName":"삼성전자","market":"KOSPI"}'
```

기본적으로 등록일의 직전 거래일까지 완료된 최근 120거래일 일봉을
`daily_prices`에 upsert하고, 60개 이상 확보되면 MA5/MA20/MA60, RSI(14),
MACD, Bollinger Band를 계산해 `indicator_snapshots`에 저장합니다. KIS
조회 실패나 데이터 부족은 `warmUp.status`, `warnings`와
`indicator_warmup_histories`에 기록되며 관심종목 등록 자체는 성공합니다.

수동 warmup 및 조회:

```sh
curl -X POST \
  'http://localhost:8080/api/indicators/warm-up?stockCode=005930'
curl -X POST \
  'http://localhost:8080/api/indicators/warm-up/active-stocks'
curl \
  'http://localhost:8080/api/indicators/warm-up/histories?stockCode=005930'
curl \
  'http://localhost:8080/api/indicators/snapshots?stockCode=005930&tradeDate=2026-06-12'
```

warmup 설정:

```text
INDICATOR_WARMUP_ENABLED=true
INDICATOR_WARMUP_LOOKBACK_TRADING_DAYS=120
INDICATOR_WARMUP_MIN_REQUIRED_DAYS_FOR_MA60=60
INDICATOR_WARMUP_FAIL_STRATEGY_WHEN_INSUFFICIENT=false
INDICATOR_WARMUP_MAX_SYMBOLS_PER_RUN=100
```

MA20 충분성은 일봉 20개, MA60 및 전체 indicator snapshot 충분성은 기본
일봉 60개로 판단합니다. 목표 120거래일이 이미 저장되어 있으면 KIS를 다시
조회하지 않고 저장 데이터로 지표를 재계산합니다.

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

14:00 예비 스캔은 ranking universe를 확보한 직후 후보 일봉과 지표를
warmup합니다. 15:00 최종 리뷰는 저장된 예비 후보를 다시 확인합니다.
장초반도 08:30 ranking 후보와 09:05 예비 후보에 같은 정책을 적용합니다.
한 실행에서 처리하는 종목은 `INDICATOR_WARMUP_MAX_SYMBOLS_PER_RUN`으로
제한합니다.

기본 설정에서는 warmup 실패나 MA60 부족 시
`INDICATOR_DATA_INSUFFICIENT` reason을 남기고 전략을 계속합니다.
`INDICATOR_WARMUP_FAIL_STRATEGY_WHEN_INSUFFICIENT=true`일 때만 해당 종목을
후보에서 제외합니다. MA20이 MA60보다 높으면
`MA20_ABOVE_MA60_UPTREND`, 현재가가 MA60 아래면
`RISK_CURRENT_PRICE_BELOW_MA60`, `MA5 < MA20 < MA60` 역배열이면
`MA5_MA20_MA60_BEARISH_ALIGNMENT` reason과 10점 감점을 적용합니다.
이 과정은 신호 분석만 수행하며 실매매나 자동 주문을 실행하지 않습니다.

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
- `tradeguard.research.earnings_analysis.count`: `result=success|insufficient|failure`
- `tradeguard.research.financial_import.count`: `result=saved|failure`
- `tradeguard.research.valuation_import.count`: `result=saved|failure`
- `tradeguard.research.valuation_auto_snapshot.count`: `result=success|insufficient|failure`
- `tradeguard.research.shares_outstanding.count`: `result=saved|failure`
- `tradeguard.research.dart_corp_code_import.count`: `result=success|partial|failure|skipped`
- `tradeguard.research.shares_outstanding_import.count`: `result=success|partial|failure`
- `tradeguard.research.catalyst_evidence.count`: `type`, `confidence`
- `tradeguard.research.disclosure_evidence_import.count`: `provider`, `result=success|partial|failure|skipped`
- `tradeguard.research.market_index_import.count`: `provider`, `result=success|partial|failure|skipped`
- `tradeguard.research.sector_import.count`: `result=success|partial|failure`
- `tradeguard.research.investor_flow_import.count`: `scope=stock|market`, `result=success|partial|failure|skipped`
- `tradeguard.research.investor_flow_readiness.count`: `result=ready|not_ready`
- `tradeguard.research.supply_demand_analysis.count`: `result=success|insufficient|failure`
- `tradeguard.strategy.supply_demand_adjustment.count`: `strategy=closing_bet|early_market`, `result=strong|distribution|insufficient|neutral`
- `tradeguard.research.earnings_event.count`: `status`
- `tradeguard.research.earnings_preview.count`: `result=created|ready|failure`
- `tradeguard.research.post_earnings_review.count`: `thesisImpact`

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
- Earnings Analysis는 재무 품질 평가와 후보 reason/점수 보강만 수행하며 자동 주문을 실행하지 않습니다.
- Investor Flow import와 Supply-Demand 분석은 저장 데이터와 후보 점수만 보강하며 자동 주문을 실행하지 않습니다.
- 09:20 follow-up 결과 저장과 조회는 분석 데이터만 다루며 자동 주문을 실행하지 않습니다.
- 장초반 전략 성과 리포트는 조회와 집계만 수행하며 신호 또는 주문을 생성하지 않습니다.
- 장초반 성과 캡처는 분석 데이터만 저장하며 주문을 생성하지 않습니다.
- 장초반 원천 데이터 아카이브는 replay 입력만 저장하며 주문을 생성하지 않습니다.
- 시간외 데이터 연동은 fake/disabled 또는 설정 기반 KIS read-only 일별 시간외 시세 adapter만 사용합니다.
- KIS 현금 지정가 주문과 현금 체결조회만 사용하며 정정/취소는 아직 구현하지 않습니다.
- API Key, App Secret, 계좌번호는 코드에 하드코딩하지 않습니다.
- Discord Webhook URL은 환경변수로만 주입하며 코드에 하드코딩하지 않습니다.
- 기존 mock order와 `FakeBrokerAdapter` 경로는 그대로 유지됩니다.
## Replay Backtest v1

Replay Backtest는 과거 후보 선정 시점에 DB에 저장된 데이터만 사용해 종가베팅과 장초반 전략의 후보 점수, reason, 이후 성과를 재현한다. 실행 중 KIS, DART 또는 다른 provider를 호출하지 않으며 BrokerPort나 주문 서비스와 연결되지 않는다. 따라서 자동매수·자동매도와 완전히 분리된 연구 기능이다.

v1의 후보 재현 소스는 저장된 `trading_signals`다. 이 레코드에 후보 선정 당시 기술지표, 시장지수, 섹터, 실적, valuation, supply-demand 반영 결과인 score와 reasons가 보존된다. 성과 가격은 다음 저장 데이터에서만 조회한다.

- 종가베팅: `daily_prices`의 신호일 종가와 N번째 후속 거래일 종가
- 장초반: `early_market_intraday_bar_snapshots`의 entry/exit 시각 이하 최신 bar 종가
- 종목명: `stocks`

요청한 기준 가격이 없으면 실행을 중단하지 않고 해당 후보를 `DATA_INSUFFICIENT`로 저장한다. 유효한 성과만 평균, 중앙값, 승률, 최대·최소 수익률에 포함한다. reason 및 warning별 후보 수, 평가 수, 승률, 평균 수익률도 run 조회 응답에 포함된다.

```sh
curl -X POST 'http://localhost:8080/api/research/backtests/replay/closing-bet?from=2026-06-01&to=2026-06-15&holdingDays=1'

curl -X POST 'http://localhost:8080/api/research/backtests/replay/early-market?from=2026-06-01&to=2026-06-15&entryTime=09:05&exitTime=09:31'

curl 'http://localhost:8080/api/research/backtests/replay/runs/1'
curl 'http://localhost:8080/api/research/backtests/replay/runs/1/results'
```

운영 metric은 `tradeguard.research.replay_backtest.count`이며 `strategy`와 `result=success|failure|insufficient`만 tag로 사용한다. `stockCode`는 metric tag에 포함하지 않는다.

## Live Paper Trading Report v1

Paper Trading Report는 실제 주문이나 모의 체결을 생성하지 않고, 당일 저장된 후보를 reference price 기준으로 평가하는 장후 연구 리포트다. KIS, DART 및 다른 provider를 호출하지 않으며 `BrokerPort`나 주문 서비스에 의존하지 않는다.

- 장초반: 저장된 09:05 이하 최신 bar를 진입가, 09:31 이하 최신 bar를 청산가로 사용하고 해당 구간의 고가·저가로 MFE/MAE를 계산한다. bar가 부족하면 기존 장초 성과 캡처를 fallback으로 사용한다.
- 종가베팅: 당일 종가를 진입가로 사용한다. 기본 `NEXT_CLOSE` 정책은 다음 거래일 종가, 선택 가능한 `NEXT_OPEN` 정책은 다음 거래일 시가를 청산가로 사용한다.
- Morning Note 관심 후보: 활성 관심종목의 당일 시가와 종가를 사용한다.

16:10 당일 자동 실행에서는 다음 거래일 가격이 아직 없으므로 종가베팅 결과가 `DATA_INSUFFICIENT`일 수 있다. 이후 과거 날짜를 다시 생성하면 저장된 다음 거래일 데이터로 평가된다. 유효한 결과만 승률과 평균 수익률에 포함하며 전략/reason/warning별 집계와 top winners/losers를 제공한다.

```sh
curl -X POST 'http://localhost:8080/api/research/paper-trading/reports/daily?tradeDate=2026-06-15'
curl 'http://localhost:8080/api/research/paper-trading/reports/latest?tradeDate=2026-06-15'
curl 'http://localhost:8080/api/research/paper-trading/reports/runs/1'
curl 'http://localhost:8080/api/research/paper-trading/reports/runs/1/results'
```

`PAPER_TRADING_REPORT_AUTO_RUN=true`이면 거래일 16:10 Asia/Seoul에 실행한다. 다음날 Morning Note에는 전 거래일 후보 수, 승률, 평균 수익률, 최고 reason, 최저 warning과 `DATA_INSUFFICIENT` 건수가 포함되며 Discord 전송은 기존 Morning Note opt-in 설정을 그대로 따른다.

운영 metric은 `tradeguard.research.paper_trading_report.count`이고 `result=success|failure|insufficient`만 tag로 사용한다. `stockCode`는 metric tag에 포함하지 않는다.

## Disclosure Actual Provider v1

Disclosure Actual Provider는 관심종목과 보유종목의 OpenDART 공시검색 결과에서
제목, 접수일, 공시 분류, 접수번호 기반 URL 등 필요한 metadata만 수집한다.
공시 원문, HTML, 첨부파일, API 응답 전문은 저장하지 않으며 뉴스 크롤링도 수행하지 않는다.
수집 evidence는 Morning Note와 Operational Dashboard에만 반영되고 주문 서비스와 연결되지 않는다.

```env
DISCLOSURE_ACTUAL_PROVIDER_ENABLED=false
DISCLOSURE_ACTUAL_PROVIDER_TYPE=DART
DISCLOSURE_ACTUAL_PROVIDER_TIMEOUT_SECONDS=10
DISCLOSURE_ACTUAL_PROVIDER_AUTO_RUN=false
DISCLOSURE_ACTUAL_PROVIDER_LOOKBACK_DAYS=7
DISCLOSURE_ACTUAL_PROVIDER_MAX_ITEMS_PER_STOCK=20
DISCLOSURE_ACTUAL_PROVIDER_RATE_LIMIT_PER_MINUTE=30
```

`DART_PROVIDER_ENABLED=true`, `DART_API_BASE_URL`, `DART_API_KEY`와 종목별 DART
corp code mapping도 필요하다. 자동 실행은 별도 opt-in이며 거래일 07:35에 수행된다.

```sh
curl -X POST 'http://localhost:8080/api/research/disclosures/import?stockCode=005930&from=2026-06-01&to=2026-06-15'
curl -X POST 'http://localhost:8080/api/research/disclosures/import-watchlist?baseDate=2026-06-15'
curl 'http://localhost:8080/api/research/disclosures/import-histories?stockCode=005930'
curl 'http://localhost:8080/api/research/disclosures/evidences?stockCode=005930&from=2026-06-01&to=2026-06-15'
```

Morning Note에는 `NEW_DISCLOSURE_EVIDENCE`, `HIGH_IMPORTANCE_DISCLOSURE`,
`DISCLOSURE_IMPORT_FAILED`, `DISCLOSURE_PROVIDER_DISABLED`,
`DISCLOSURE_EVIDENCE_MISSING_FOR_HIGH_CATALYST`가 필요할 때 추가된다.
수주·공급계약 및 실적 관련 제목만 catalyst 후보로 보수적으로 연결하며 thesis 상태나 주문 상태는 변경하지 않는다.

Operational Dashboard의 `dartStatus`에는 다음과 같은 disclosure 상태가 함께 노출된다.

```json
{
  "disclosureProviderEnabled": true,
  "latestDisclosureImportStatus": "SUCCESS",
  "failedDisclosureImportCount": 0,
  "highImportanceDisclosureCount": 2
}
```

## Consensus Provider v1

Consensus Provider v1은 종목별 분기 실적 기대치와 목표주가 컨센서스 스냅샷을
리서치 데이터로 저장합니다. 입력은 운영자 수동 입력, CSV, 또는 사용 권한이
확인된 합법 provider만 허용합니다. 무단 웹 크롤링, 증권사 리포트 원문 저장,
유료 데이터 무단 수집은 지원하지 않습니다. 외부 provider port는 향후 연동을
위해 분리되어 있지만 v1 adapter는 disabled 상태로 빈 결과만 반환합니다.

기본 설정은 다음과 같습니다.

```text
CONSENSUS_PROVIDER_ENABLED=false
CONSENSUS_PROVIDER_TYPE=CSV
CONSENSUS_PROVIDER_AUTO_RUN=false
CONSENSUS_PROVIDER_TIMEOUT_SECONDS=10
CONSENSUS_PROVIDER_LOOKBACK_DAYS=90
CONSENSUS_PROVIDER_MAX_ITEMS_PER_STOCK=20
```

실적 컨센서스 CSV import 예시:

```sh
curl -X POST 'http://localhost:8080/api/research/consensus/earnings/import-csv' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@earnings-consensus.csv'
```

```csv
stockCode,fiscalYear,fiscalQuarter,consensusDate,expectedRevenue,expectedOperatingIncome,expectedNetIncome,expectedOperatingMargin,analystCount,source,providerName
005930,2026,2,2026-06-15,80000000000000,12000000000000,9000000000000,15.0,18,CSV,licensed-provider
```

목표주가 컨센서스 CSV import 예시:

```sh
curl -X POST 'http://localhost:8080/api/research/consensus/target-price/import-csv' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@target-price-consensus.csv'
```

```csv
stockCode,consensusDate,targetPrice,currentPrice,upsideRate,analystCount,source,providerName
005930,2026-06-15,90000,75000,20.0,18,CSV,licensed-provider
```

수동 저장은 `POST /api/research/consensus/earnings`와
`POST /api/research/consensus/target-price`, 조회는 각 경로의 `GET`을 사용합니다.
Earnings Preview 생성 시 해당 분기의 최신 컨센서스가 있으면 expected 값과
사용 근거를 채웁니다. Post Earnings Review는 preview expected 값이 없는
항목만 컨센서스로 보완하여 매출·영업이익·순이익 surprise rate를 계산하고
컨센서스 사용 여부를 기록합니다.

Morning Note에는 `CONSENSUS_AVAILABLE`, `CONSENSUS_STALE`,
`TARGET_PRICE_UPSIDE_HIGH`, `TARGET_PRICE_DOWNSIDE_WARNING`,
`EARNINGS_CONSENSUS_MISSING`을 표시합니다. Operational Dashboard의
`consensusStatus`는 실적/목표주가 건수, stale 건수, 예정 실적 중 누락 건수와
경고를 제공합니다. 이 데이터는 종베·장초 전략 점수 및 Broker/주문 경로와
연결되지 않으며 자동매수·자동매도·실계좌 주문을 실행하지 않습니다.
