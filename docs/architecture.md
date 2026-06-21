# TradeGuard Architecture

## 1. 목적과 범위

TradeGuard는 한국 주식의 일봉·장중 데이터를 분석해 매매 후보를 만들고, 운영자가 선택한 계좌와 리스크 정책을 통과한 수동 지정가 요청만 KIS 모의/실전 주문 어댑터로 전달할 수 있는 Spring Boot 애플리케이션이다.

현재 MVP의 경계는 다음과 같다.

- 분석, 신호 생성, 리스크 판정, 모의 주문과 운영자 확인형 실전 지정가 주문을 지원한다.
- 자동매수는 지원하지 않으며 전략·scheduler·alert가 주문을 직접 만들지 않는다.
- 시장가, 신용, 미수, 공매도 주문은 지원하지 않는다.
- 계좌와 KIS/DART 자격정보는 DB 암호문으로 관리하고 암호화 키만 로컬 환경에서 주입한다.
- 로컬 기본 진입점은 `http://localhost:18080`이다.

## 2. 아키텍처 스타일

프로젝트는 Hexagonal Architecture를 지향한다. 핵심 원칙은 비즈니스 규칙을 프레임워크와 외부 시스템으로부터 분리하는 것이다.

```text
Inbound Adapter
  REST Controller
        |
        v
Inbound Port
  Use Case Interface
        |
        v
Application Service
        |
        +----------> Domain Model / Policy
        |
        v
Outbound Port
  Persistence / Broker Port
        |
        v
Outbound Adapter
  JPA / Fake Broker / KIS market-data and order adapters
```

의존성 방향은 항상 외부에서 내부를 향해야 한다.

```text
adapter -> application -> domain
config  -> application/domain
domain  -X-> Spring, JPA, Web, Broker SDK
```

## 3. 패키지 책임

| 패키지 | 책임 | 허용되는 의존성 |
| --- | --- | --- |
| `domain.stock` | 관심종목과 시장 모델 | Java 표준 라이브러리 |
| `domain.market` | 일봉 가격 모델 | Java 표준 라이브러리 |
| `domain.indicator` | MA, RSI, MACD, Bollinger Band 계산 | 다른 domain 타입 |
| `domain.strategy` | 전략 점수화와 거래 신호 | 다른 domain 타입 |
| `domain.risk` | 주문 전 리스크 정책 | 다른 domain 타입 |
| `domain.order` | 주문 요청과 상태 | Java 표준 라이브러리 |
| `application.port.in` | 외부에서 호출하는 유스케이스 계약 | domain |
| `application.port.out` | DB와 브로커에 대한 출력 계약 | domain |
| `application.service` | 유스케이스 조합과 트랜잭션 경계 | port, domain |
| `adapter.web` | HTTP 요청/응답 변환 | inbound port |
| `adapter.persistence` | JPA 매핑과 저장소 구현 | outbound port, domain |
| `adapter.broker` | 모의 브로커 구현 | outbound port, domain |
| `adapter.broker.kis` | KIS 브로커 연동 경계 | outbound port, domain |
| `adapter.marketdata.kis` | DB 환경 설정 기반 KIS OAuth, 시세 조회와 수동 지정가 주문 | outbound port, domain |
| `adapter.notification` | Discord Webhook 등 알림 전송 경계 | outbound port |
| `config` | 순수 도메인 객체의 Spring Bean 조립 | application, domain |

## 4. 핵심 도메인

### 시장 데이터와 지표

- `Stock`: 종목 코드, 종목명, 시장, 활성 여부
- `DailyPrice`: 종목별 거래일 OHLCV와 거래대금
- `IndicatorSnapshot`: MA5/20/60, RSI(14), MACD, Bollinger Band 결과
- `TechnicalIndicatorCalculator`: 입력 일봉을 날짜순으로 정렬해 지표를 계산하는 순수 Java 클래스

지표 스냅샷 전체를 만들려면 MA60 때문에 최소 60개의 일봉이 필요하다. MACD 단독 계산은 최소 35개, RSI(14)는 15개, Bollinger Band(20)는 20개가 필요하다.

### 전략과 신호

`ClosingBetStrategy`는 최근 20개 이상의 일봉과 계산된 지표를 입력받아 `TradingSignal`을 만든다. 전략은 주문을 실행하지 않으며 점수와 근거만 생성한다.

신호 상태는 다음 순서로 이동한다.

```text
CREATED -> RISK_APPROVED -> ORDER_REQUESTED
       \-> RISK_REJECTED
```

### 주문과 브로커

`OrderRequest`는 현재 `LIMIT` 주문만 생성할 수 있다. `FakeBrokerAdapter`는 요청을 즉시 `ACCEPTED`로 바꾸고 `FAKE-` 접두사의 주문번호를 발급한다.

```text
CREATED -> REQUESTED -> ACCEPTED
                     \-> REJECTED

향후 상태: PARTIALLY_FILLED, FILLED, CANCELED
```

`KisLiveTradingOrderAdapter`는 두 live-trading flag가 활성화된 경우에만 Bean으로 등록된다. 선택 계좌 환경에 따라 REAL/DEMO base URL과 TR ID를 분리하고, 실전 주문은 UI 확인값·readiness·kill switch·장시간·금액 한도를 모두 통과해야 한다.

### 운영 UI와 실시간 차트

- `/operations/dashboard`: 저장된 현재 운영 상태를 읽는 서버 렌더링 UI
- `/operations/accounts`: 다중 계좌와 환경별 KIS/DART 설정 관리 UI
- `/operations/watchlist`: 관심종목, 저장 일봉/지표, 재료, 포지션과 수동 지정가 주문 UI
- `/api/stocks/chart/stream`: 동일 종목 조회를 공유하는 SSE 현재가 스트림. 평일 09:00~15:30에만 provider를 조회하고 기본 5초 간격·3종목 한도를 적용한다.

SSE v1은 KIS 현재가 REST snapshot을 사용한다. KIS native WebSocket 체결 스트림은 아직 구현하지 않았다.

매매 화면의 호가 스트림은 이 차트 흐름과 분리된다. 서버의
`KisStreamingStockOrderBookAdapter`가 선택 계좌의 DEMO/REAL 환경에 맞는 KIS
WebSocket에 연결해 `H0STASP0`과 `H0STCNT0`을 구독한다. 브라우저는 KIS 접속키나
credential을 받지 않고 `/api/stocks/orderbook/stream` SSE에서 정제된 10단계 호가와
현재가만 수신한다. 이 읽기 전용 스트림은 BrokerPort와 주문 서비스를 호출하지 않는다.

## 5. 주요 처리 흐름

### 관심종목

```text
POST /api/stocks
  -> StockController
  -> StockService
  -> StockPort
  -> StockPersistenceAdapter
  -> StockJpaRepository
  -> stocks
```

### 분석에서 모의 주문과 브리핑까지의 목표 흐름

```text
DailyPrice 수집/조회
  -> TechnicalIndicatorCalculator
  -> IndicatorSnapshot 저장
  -> ClosingBetStrategy
  -> TradingSignal 저장
  -> RiskManager
  -> OrderService
  -> FakeBrokerAdapter
  -> OrderRequest 저장

TradingSignal 조회
  -> ClosingBetBriefingService
  -> NotificationPort
  -> DiscordWebhookNotificationAdapter

MarketRankingPort
  -> ClosingBetCandidateScanner
  -> TradingSignal 저장(CLOSING_BET_PRE_SCAN)
  -> NotificationPort

TradingSignal 조회(CLOSING_BET_PRE_SCAN)
  -> MarketSnapshotPort
  -> ClosingBetFinalReviewService
  -> TradingSignal 저장(CLOSING_BET)
  -> NotificationPort
```

`AnalyzeStockUseCase`는 기준일까지 최근 1년 일봉을 조회하고, 최소 60개가 있으면 지표와 종가베팅 신호를 계산해 저장한다. `ClosingBetCandidateScanner`는 시장 순위 후보군에서 14:00 예비 후보를 선별해 `CLOSING_BET_PRE_SCAN` 신호로 저장한다. `ClosingBetFinalReviewService`는 15:00에 예비 신호와 intraday snapshot을 다시 조회해 최종 후보를 `CLOSING_BET` 신호로 승격 저장한다. 알림 흐름은 Discord Webhook을 사용할 수 있지만 주문을 실행하지 않는다.

`MarketRankingPort`와 `MarketSnapshotPort`는 기본 fake adapter를 사용한다. `tradeguard.market-data.realtime-provider=kis`일 때만 KIS 읽기 전용 순위/current price adapter가 Bean으로 등록된다.

14:00/15:00 및 장초반 scheduler는 실행 전에 `MarketCalendarPort`를 조회한다. `ConfigurableKoreanMarketCalendarAdapter`는 `MANUAL_OVERRIDE`, `KRX_OFFICIAL`, `FALLBACK_GENERATED`, runtime 주말+설정 휴일 순서의 calendar 정책을 사용한다. 수동 보정 row는 후속 sync upsert가 덮어쓰지 않는다. 이전/다음 거래일, 08:30 시간외 기준일과 기간 리포트 거래일 수도 같은 port를 사용한다. REST 수동 전략 실행 경로는 calendar를 통과하지 않으므로 휴장일에도 호출할 수 있다.

```text
MARKET_CALENDAR_SYNC (04:00 Asia/Seoul)
  -> SyncMarketCalendarUseCase
  -> KrxMarketCalendarSyncProvider
       -> KrxMarketCalendarClient
       -> KrxMarketCalendarParser
  -> failure: FallbackGeneratedMarketCalendarSyncProvider
  -> MarketCalendarDayPort
  -> market_calendar_days
```

KRX 공식 endpoint는 adapter 내부에 격리한다. 기본 endpoint는 비어 있으며 안정적으로 검증된 endpoint가 없으면 공식 provider가 실패하고 fallback provider가 연중 전체 날짜를 생성한다. 외부 raw payload는 운영 info log에 남기지 않는다. 이 흐름은 주문 port를 호출하지 않는다.

```text
PATCH market calendar day
  -> OverrideMarketCalendarDayUseCase
  -> MarketCalendarDayPort (MANUAL_OVERRIDE)
  -> MarketCalendarDayAuditPort
  -> market_calendar_day_audits
```

보정 row 저장과 audit 저장은 하나의 transaction이다. Validation은 연중 완전성, 주말 거래일, 평일 휴장일, source 분포와 향후 30일 거래일 존재 여부를 확인한다. Calendar 관리 흐름은 주문 port를 호출하지 않는다.

### KIS 일봉 수집

```text
ImportDailyPricesUseCase
  -> MarketDataImportService
  -> MarketDataPort
  -> KisMarketDataAdapter
  -> KIS 모의투자 기간별 시세 API
  -> DailyPricePort
  -> daily_prices
```

KIS adapter는 DB의 환경별 설정과 현재 기본 계좌를 사용해 모의/실전 호스트를 구분한다. OAuth 토큰은 REAL/DEMO별 MEMORY 또는 AES-256-GCM DB cache를 사용하며 DB 모드에서는 refresh lease로 중복 발급을 막는다. 기간별 시세 API는 100건 단위로 분할 조회한다.

KIS 시장 순위와 current price adapter도 같은 인증 정보를 사용하지만 주문 endpoint를 호출하지 않는다. 현재 사용 경로는 국내주식 거래량순위, 등락률순위, 주식현재가 시세 조회뿐이다.

## 6. 영속성 설계

| 테이블 | 식별 기준 | 용도 |
| --- | --- | --- |
| `stocks` | `stock_code` | 관심종목 |
| `daily_prices` | `stock_code + trade_date` | 일봉 |
| `market_calendar_days` | `market + trade_date` | KRX 주식시장 거래일/휴장일과 source |
| `market_calendar_day_audits` | 생성 ID | calendar 수동 보정 before/after 이력 |
| `indicator_snapshots` | 생성 ID, `stock_code + trade_date` unique | 일자별 기술지표 |
| `trading_signals` | 생성 ID, 전략+종목+일자+유형 unique | 전략 신호와 상태 |
| `trading_signal_reasons` | signal FK | 점수 근거 목록 |
| `order_requests` | 생성 ID | 전략 기반 모의 주문 이력 |
| `trading_accounts` | 생성 ID | REAL/DEMO 다중 거래 계좌 암호문과 기본 계좌 |
| `kis_api_configurations` | 환경 | 환경별 KIS base URL과 자격정보 암호문 |
| `dart_api_configurations` | 단일 row | DART base URL과 API key 암호문 |
| `live_orders`, `live_positions` | 생성 ID | 수동 KIS 주문, 체결과 환경별 포지션 |
| `quarterly_financials` | `stock_code + fiscal_year + fiscal_quarter` | 운영자 입력 분기 재무 |
| `valuation_snapshots` | `stock_code + trade_date` | 운영자 입력 valuation snapshot |
| `earnings_analysis_snapshots` | `stock_code + base_date` | 실적 품질/valuation 점수와 상태 |

운영 DB 모델과 로컬 기본 실행은 MySQL을 사용한다. 일반 테스트만
`test` profile의 `src/test/resources/application-test.properties`에서 H2
MySQL mode를 사용한다.
로컬 실행과 운영 모두 `JPA_DDL_AUTO=validate`와 Flyway migration을
사용한다.

## 7. 트랜잭션과 일관성

`OrderService.request()`가 주문 유스케이스의 트랜잭션 경계다. 리스크 확인, 브로커 요청, 신호와 주문 저장이 한 흐름에 포함된다.

모의 주문 중복 방지는 애플리케이션 검사와 DB unique constraint를 함께 사용한다. 브로커 실패 이력, 재시도와 감사 이력도 저장한다. 다만 외부 KIS 호출과 DB commit은 하나의 원자적 트랜잭션이 아니므로 장애 시 reconciliation이 필요하다. 계좌 선택 주문은 현재 로컬 단일 운영자용 JVM 직렬화이며 `live_orders`에 선택한 account ID를 저장하지 않으므로, 다중 사용자·다중 인스턴스 전환 전에 계좌 식별자 영속화와 DB 수준 동시성 제어가 필요하다.

## 8. 보안과 운영 원칙

- `.env`와 실제 인증정보는 Git에 저장하지 않는다.
- `.env.example`에는 키 이름과 안전한 예시만 둔다.
- 로그에 App Key, App Secret, 계좌번호, 토큰을 출력하지 않는다.
- 실전 주문은 운영자가 REAL 계좌를 명시 선택하고 확인값을 보낸 수동 지정가 요청만 허용한다.
- 자동매수와 전략/scheduler/alert 기반 주문은 금지한다.
- KIS 시세 조회와 주문 연동은 별도 adapter와 port로 분리한다.
- 외부 API 장애가 도메인 계산과 테스트에 영향을 주지 않도록 한다.

## 9. 알려진 아키텍처 부채

- 매매 호가는 KIS native WebSocket을 사용한다. 차트는 아직 제한된 REST snapshot을 SSE로 전달한다.
- Watchlist는 선택 계좌의 KIS 잔고를 읽기 전용으로 표시한다. KIS 잔고와 TradeGuard 저장 포지션의 자동 대사는 아직 없다.
- 계좌 선택 주문은 로컬 단일 운영자용 JVM 직렬화 방식이다. 다중 사용자 전환 시 주문에 account ID를 영속화해야 한다.
- KRX calendar 공식 endpoint가 비어 있으면 생성 fallback을 사용하므로 임시휴장일 운영 검증이 필요하다.

새 기능은 이 부채를 확대하지 않고 port 중심으로 구현한다.

## 10. Earnings Analysis v1

```text
POST /api/research/financials/quarterly
  -> EarningsDataUseCase
  -> QuarterlyFinancialPort
  -> quarterly_financials

POST /api/research/valuations
  -> EarningsDataUseCase
  -> ValuationSnapshotPort
  -> valuation_snapshots

POST /api/research/earnings-analysis
  -> AnalyzeEarningsUseCase
  -> QuarterlyFinancialPort + ValuationSnapshotPort
  -> EarningsAnalysisPort
  -> earnings_analysis_snapshots
```

Earnings Analysis는 최근 분기와 전년 동기 재무를 비교해 성장률, 마진,
부채비율, 현금흐름, PER/PBR/PSR 점수를 저장한다. 외부 뉴스, 공시,
컨센서스 provider는 아직 연결하지 않는다.

`ClosingBetCandidateScanner`와 `EarlyMarketPreOpenScanner`는 최신
`EarningsAnalysisSnapshot`을 읽어 STRONG 가점, WEAK 감점 또는 선택적 제외,
DATA_INSUFFICIENT reason만 반영한다. 이 경로는 `OrderService`,
`BrokerPort`, KIS 주문 adapter를 호출하지 않는다.

## Operational Dashboard v1

`OperationalDashboardController`는 `GetOperationalDashboardUseCase`만 호출한다. 구현 서비스는 기존
application port를 통해 저장된 calendar, scheduler history, research snapshot, paper report,
token 및 readiness 데이터를 읽어 하나의 `OperationalDashboardSummary`로 조합한다.

- 외부 KIS/DART/provider 호출 없음
- 주문 생성 및 자동매매 상태 변경 없음
- 조회할 수 없는 상태는 성공으로 추정하지 않고 `UNAVAILABLE`, `NOT_RUN` 또는 warning으로 노출
- 운영 차단 조건과 권장 조치는 서비스의 단일 정책 계층에서 계산
