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

모의 주문 API는 DB에 저장된 신호만 사용하며 지정가 주문만 생성합니다. 주문 이력 응답에는 `orderId`가 포함되며, 필터를 생략하면 전체 주문 이력을 최신 거래일 순으로 반환합니다.

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

## 안전 원칙

- 실계좌 주문 기능은 구현하지 않습니다.
- 시장가 주문은 지원하지 않습니다.
- API Key, App Secret, 계좌번호는 코드에 하드코딩하지 않습니다.
- Discord Webhook URL은 환경변수로만 주입하며 코드에 하드코딩하지 않습니다.
- `KisBrokerAdapter`는 스켈레톤만 제공하며 실제 주문 API를 호출하지 않습니다.
