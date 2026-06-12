# TradeGuard Architecture

## 1. 목적과 범위

TradeGuard는 한국 주식의 일봉 데이터를 분석해 매매 후보를 만들고, 리스크 정책을 통과한 요청만 모의 브로커로 전달하는 Spring Boot 애플리케이션이다.

현재 MVP의 경계는 다음과 같다.

- 분석, 신호 생성, 리스크 판정, 모의 주문 요청을 지원한다.
- 실제 한국투자증권 주문 API는 호출하지 않는다.
- 실계좌 주문과 시장가 주문은 지원하지 않는다.
- 외부 비밀정보는 환경변수로만 주입한다.

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
  JPA / Fake Broker / KIS Skeleton
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
| `adapter.broker.kis` | KIS 연동 경계와 스켈레톤 | outbound port, domain |
| `adapter.marketdata.kis` | KIS 모의투자 OAuth, 읽기 전용 일봉/순위/current price 조회 | outbound port, domain |
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

`KisBrokerAdapter`는 의도적으로 Spring Bean으로 등록하지 않았으며, 호출 시 `UnsupportedOperationException`을 발생시킨다.

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

KIS adapter는 모의투자 호스트만 허용한다. OAuth 토큰은 메모리에 캐시하며, 만료 1분 전부터 새 토큰을 발급한다. 기간별 시세 API는 한 번에 최대 100건을 반환하므로 응답이 잘린 경우 날짜 범위를 나눠 다시 호출해야 한다.

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
| `order_requests` | 생성 ID | 모의 주문 이력 |

운영 DB 모델은 MySQL을 기준으로 하며 로컬 기본 실행과 테스트는 H2 MySQL mode를 사용한다. 운영에서는 `JPA_DDL_AUTO=validate`와 명시적인 DB migration 도구 사용을 목표로 한다.

## 7. 트랜잭션과 일관성

`OrderService.request()`가 주문 유스케이스의 트랜잭션 경계다. 리스크 확인, 브로커 요청, 신호와 주문 저장이 한 흐름에 포함된다.

현재 구현에서 보완해야 할 일관성 항목은 다음과 같다.

- 리스크 거절 시 예외로 트랜잭션이 롤백되어 거절 신호 이력이 남지 않을 수 있다.
- 같은 `TradingSignal`을 상태별로 저장할 때 신규 JPA Entity가 생성되어 이력이 중복 행으로 남을 수 있다.
- 중복 주문은 사전 조회만 수행하므로 동시 요청을 막는 DB unique constraint가 필요하다.
- 브로커 호출과 DB commit 사이의 실패를 다루는 idempotency 및 복구 정책이 필요하다.

이 항목들은 모의 주문 API를 외부에 노출하기 전에 해결해야 한다.

## 8. 보안과 운영 원칙

- `.env`와 실제 인증정보는 Git에 저장하지 않는다.
- `.env.example`에는 키 이름과 안전한 예시만 둔다.
- 로그에 App Key, App Secret, 계좌번호, 토큰을 출력하지 않는다.
- `tradeguard.real-trading-enabled` 값과 관계없이 MVP는 실주문을 실행하지 않는다.
- KIS 시세 조회와 주문 연동은 별도 adapter와 port로 분리한다.
- 외부 API 장애가 도메인 계산과 테스트에 영향을 주지 않도록 한다.

## 9. 알려진 아키텍처 부채

- 일봉은 KIS 수집과 저장 유스케이스가 있으나 REST API와 다중 구간 pagination이 없다.
- KIS read-only smoke test는 환경변수 opt-in 방식이며 CI 기본 테스트에서는 실행되지 않는다.
- 휴장일은 정적 설정 목록이며 한국거래소 calendar 자동 동기화는 없다.
- 도메인 상태 전이에 대한 허용 순서 검증이 없다.
- 관측성 구성이 없다.

새 기능은 이 부채를 확대하지 않고 port 중심으로 구현한다.
