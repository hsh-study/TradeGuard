# Architecture

TradeGuard는 Hexagonal Architecture 스타일을 따릅니다.

## Package Layout

- `domain`: Spring에 의존하지 않는 순수 도메인 모델과 정책
- `application.port.in`: 유스케이스 진입 포트
- `application.port.out`: 저장소와 브로커 같은 외부 의존 포트
- `application.service`: 유스케이스 조합과 트랜잭션 경계
- `adapter.web`: REST API
- `adapter.persistence`: JPA Entity, Repository, Persistence Adapter
- `adapter.broker`: FakeBrokerAdapter 및 KIS 스켈레톤
- `config`: 도메인 Bean 설정

## Trading Flow

1. `ClosingBetStrategy`가 일봉과 지표를 바탕으로 `TradingSignal`을 만든다.
2. `RiskManager`가 점수, 주문 금액, 수량, 주문 유형, 중복 주문을 검증한다.
3. 승인된 신호만 `OrderService`를 통해 주문 요청으로 변환된다.
4. MVP에서는 `FakeBrokerAdapter`가 모의 broker order number를 발급한다.
5. `KisBrokerAdapter`는 실제 주문 API 호출 없이 명시적으로 `UnsupportedOperationException`을 던진다.

## Persistence

JPA 엔티티는 MySQL 기준으로 설계했으며, 로컬 기본값과 테스트는 H2 MySQL mode를 사용한다. 도메인 로직은 JPA 엔티티에 의존하지 않는다.
