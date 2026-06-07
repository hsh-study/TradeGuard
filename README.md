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

KIS 모의투자 일봉 조회에는 `KIS_APP_KEY`, `KIS_APP_SECRET`이 필요합니다. 구현은 모의투자 호스트만 허용하며 실제 주문 API를 호출하지 않습니다.

## 테스트

```sh
./gradlew test
```

로컬 자격증명으로 KIS 읽기 전용 smoke test를 실행하려면:

```sh
set -a
source .env
set +a
KIS_SMOKE_TEST=true ./gradlew test --tests '*KisMarketDataSmokeTest'
```

## API

관심종목 등록:

```sh
curl -X POST http://localhost:8080/api/stocks   -H 'Content-Type: application/json'   -d '{"stockCode":"005930","stockName":"삼성전자","market":"KOSPI"}'
```

관심종목 조회:

```sh
curl http://localhost:8080/api/stocks
```

## 안전 원칙

- 실계좌 주문 기능은 구현하지 않습니다.
- 시장가 주문은 지원하지 않습니다.
- API Key, App Secret, 계좌번호는 코드에 하드코딩하지 않습니다.
- `KisBrokerAdapter`는 스켈레톤만 제공하며 실제 주문 API를 호출하지 않습니다.
