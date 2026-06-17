# TradeGuard MVP Roadmap

## 1. 목표

MVP의 목표는 실계좌 자동매매가 아니라 다음 흐름을 재현 가능하고 안전하게 검증하는 것이다.

```text
관심종목
  -> 일봉 수집/저장
  -> 기술지표 계산/저장
  -> 종가베팅 후보 점수화
  -> 리스크 판정
  -> 모의 주문
  -> 신호/주문 이력 조회와 알림
```

## 2. 완료 기준

MVP 1차는 다음 조건을 모두 만족할 때 완료로 본다.

- 외부에서 관심종목을 등록하고 조회할 수 있다.
- 종목별 일봉을 저장하고 중복 없이 조회할 수 있다.
- 저장된 데이터로 지표 스냅샷을 계산하고 저장할 수 있다.
- 전략 실행 결과와 점수 근거가 이력으로 남는다.
- 리스크 승인된 주문만 Fake Broker에 전달된다.
- 중복 주문이 애플리케이션과 DB 양쪽에서 차단된다.
- 거절 신호를 포함한 모든 판정 이력을 조회할 수 있다.
- 핵심 도메인과 주요 application flow가 자동 테스트로 보호된다.
- 실제 주문 API를 호출하는 실행 경로가 없다.
- 로컬 실행, MySQL 실행, API 사용법이 문서화되어 있다.

## 3. 현재 상태

상태 표기:

- `완료`: 코드와 기본 테스트가 존재함
- `부분 완료`: 기반 구조는 있으나 유스케이스 연결 또는 검증이 부족함
- `미구현`: 구현이 시작되지 않았거나 빈 스켈레톤만 존재함

| 영역 | 상태 | 현재 내용 |
| --- | --- | --- |
| 관심종목 등록/조회 | 부분 완료 | Port와 persistence adapter 경계 적용. validation, 중복 응답 정책이 필요 |
| 일봉 모델/저장 구조 | 부분 완료 | KIS 읽기 전용 수집, 100건 초과 분할 조회, 저장/기간 조회 존재. Web API 없음 |
| 기술지표 | 완료 | MA, RSI, MACD, Bollinger Band와 단위 테스트 존재 |
| 지표 저장 | 부분 완료 | 계산·저장 orchestration과 종목·거래일 upsert 존재. 조회 API 없음 |
| 종가베팅 전략 | 완료 | 단건·활성 관심종목 분석 API에 연결됐으며 점수 계산과 테스트 존재 |
| 14:00 예비 스캔 | 부분 완료 | Fake 또는 설정 기반 KIS read-only 시장 순위로 CLOSING_BET_PRE_SCAN 저장, 수동 API, DB 우선 시장 calendar 기반 14:00 scheduler, opt-in smoke test 존재 |
| 15:00 최종 리뷰 | 부분 완료 | MarketSnapshotPort 기반 VWAP/고가권/거래대금 재평가, 거래일 15:00 scheduler, opt-in current price smoke test 존재. 더 정교한 intraday feature는 TODO |
| 장초반 매매 후보 | 부분 완료 | 기존 08:30/09:05/09:20/09:31 분석 흐름, follow-up/성과/리포트/파라미터 실험/제한적 backtest 구현. V13에서 시장 순위, 직전 거래일 시간외 시세, 09:00~09:30 1분봉과 단계별 market snapshot을 capture 이력과 함께 아카이브하고 조회하는 API를 추가함. 동일 bar와 동일 시각 snapshot은 upsert하며 캡처 실패는 전략 실행을 막지 않고 상태·metric·warning log로 기록함. 과거 신호 재계산 replay 백테스트는 원천 데이터가 충분히 축적된 뒤 구현. 모든 기능은 자동 주문을 실행하지 않음 |
| 신호 저장 | 부분 완료 | 논리 키 upsert, 상태 갱신, 리스크 거절 사유 저장, 신호 조회 API와 주요 상태 변경 감사 이력 존재. 동시성 검증 필요 |
| RiskManager | 부분 완료 | 기본 정책과 단위 테스트 존재. 경계/복수 위반/동시성 테스트 필요 |
| 모의 주문 | 부분 완료 | 논리 키 및 signalId 기반 요청 API, order_requests.signal_id FK 추적, 승인/거절/BROKER_FAILED 결과, 동일 row 수동 재시도, 성공 시 신호 상태 동기화, RETRY_REQUESTED 정체 조회/수동 복구, 주요 상태 변경 감사 이력 존재. 자동 재시도/복구는 미구현 |
| 중복 주문 방지 | 완료 | 사전 조회, DB 복합 unique constraint, 충돌의 도메인 거절 변환과 통합 테스트 존재 |
| 알림 | 부분 완료 | Discord Webhook 기반 종가베팅 브리핑 API와 no-op 처리 존재. 일반 알림 정책은 미구현 |
| KIS 연동 | 부분 완료 | 모의투자 OAuth, 일봉/순위/current price read-only 조회와 opt-in smoke test 구현. 계좌/주문 연동은 의도적으로 제외 |
| 운영 관측성 | 부분 완료 | Actuator health/info/metrics, liveness/readiness, dependency health, scheduler 실행 이력, 핵심 Micrometer counter, 구조화 로그와 X-Request-Id 구현. 감사 이력 및 scheduler 실행 이력 correlation 연결 완료. 외부 metrics backend는 미구현 |
| 시장 calendar | 부분 완료 | V11 `market_calendar_days`, V12 보정 audit, MANUAL_OVERRIDE 우선 정책, DB 우선 scheduler skip/이전·다음 거래일/시간외 기준일/리포트 거래일 수, 연도 sync·조회·검증·보정·audit API, 04:00 누락 연도 scheduler, health/metrics 구현. 공식 provider client/parser는 분리했으나 안정적인 KRX 무인증 endpoint가 명확하지 않아 기본은 생성 fallback이며 `MARKET_CALENDAR_HOLIDAYS` runtime fallback 관리가 필요 |
| KIS 실매매 1단계 | 부분 완료 | 수동 지정가 매수/매도, 체결·부분체결 reconciliation, 취소, 자동취소 opt-in, 포지션 exit, kill switch 구현. REAL/DEMO OAuth token MEMORY cache와 AES-256-GCM DB cache, DB refresh lease, refresh scheduler, token health/API, 실매매 readiness 및 credential rotation/배포 체크리스트를 추가함. 자동매수·시장가·신용·미수·공매도는 미지원 |
| DB migration | 완료 | Flyway V1~V17 schema migration, Hibernate validate, H2 및 MySQL Testcontainers 검증 존재 |
| 일봉·지표 warmup | 완료 | 관심종목 등록 후 KIS 일봉 120거래일 upsert, MA5/20/60·RSI·MACD·Bollinger 저장, warmup 이력/API/metrics, 종베 14:00·15:00 및 장초반 08:30·09:05 후보 사전 보강과 strict 제외 정책 구현 |
| Action 1: Thesis | 완료 | `investment_theses`와 등록/종목별 조회/부분 수정/종료 API 구현. 핵심 가정, 무효화 조건, 목표가, 손절 조건, confidence, ACTIVE/WATCH/BROKEN/CLOSED 상태를 저장. BROKEN은 Morning Note action item만 만들고 자동매도하지 않음 |
| Action 2: Catalyst | 완료 | `investment_catalysts`와 종목/기간 조회, 등록/부분 수정 API 구현. 실적·정책·수주·제품·섹터·공시·매크로 catalyst, 중요도와 진행 상태를 관리. UPCOMING 항목은 Morning Note에 포함되며 자동매수하지 않음 |
| Action 3: Morning Note | 부분 완료 | 거래일 08:10 생성 scheduler와 수동 생성/조회 API 구현. 전 거래일 저장 후보, 보유 포지션, 관심종목 지표와 sector, market index 저장 데이터, 상위/하위 sector snapshot, upcoming catalyst, broken thesis, 데이터 부족 경고와 action item을 저장. Discord는 opt-in. 뉴스·공시·실적 원문 데이터 소스는 미연결 |
| Action 1 /sector: Market/Sector Master | 부분 완료 | `market_indices`, `sectors`, `stock_sector_mappings`, `sector_daily_snapshots` schema와 sector 등록/조회/매핑/snapshot 생성 API 구현. 08:05 `SECTOR_DAILY_SNAPSHOT` scheduler가 전 거래일 기준 가격·거래대금 흐름과 leading stock을 계산해 Morning Note 기반으로 사용. 시장지수 원천 수집 port/provider와 뉴스 연결은 미구현 |
| Earnings Analysis | 완료 | 운영자 입력 기반 `quarterly_financials`, `valuation_snapshots`, `earnings_analysis_snapshots`와 수동 저장/분석/조회 API 구현. Morning Note와 종베/장초 후보 reason/점수에 STRONG/WEAK/DATA_INSUFFICIENT를 반영하되 자동 주문은 실행하지 않음 |
| Earnings Preview | 완료 | 운영자 입력 기반 `earnings_events`, `earnings_previews`와 event/preview API, thesis/latest analysis/valuation/indicator/catalyst 기반 preview 초안 생성 구현. EARNINGS catalyst 자동 생성 옵션과 중복 방지 포함 |
| Earnings Post Review | 완료 | `post_earnings_reviews`와 post review API 구현. expected 대비 surprise 계산, thesis impact action item, announced-but-not-reviewed Morning Note 경고 구현. BROKEN이어도 자동 thesis 변경·자동매도 없음 |
| DART Financial Import | 완료 | 공식 OpenDART 재무제표 API 기반 import 구조, corp mapping, import history, 계정명 exact/normalized mapping, `quarterly_financials` upsert, 선택적 earnings analysis 재실행, Morning Note DART action item, metric 구현. 기본 provider disabled이며 뉴스·공시 원문·컨센서스 수집과 자동 주문은 없음 |
| Valuation Auto Snapshot | 완료 | `shares_outstanding_snapshots`와 valuation 자동 생성 API/scheduler 구현. 저장된 일봉 종가, 최신 분기 재무, 최신 발행주식수로 PER/PBR/PSR과 EPS/BPS/SPS를 계산하고 선택적으로 Earnings Analysis를 재실행. 후보 생성 중 외부 호출과 자동 주문은 없음 |
| DART Corp Mapping Import | 완료 | 공식 OpenDART corpCode zip/XML 파싱 port/provider, import history, 상장사 stock_code 기반 `dart_corp_mappings` upsert 구현. 기존 market 보존, 신규 mapping `UNKNOWN`, 원본 zip/xml DB 저장 없음, 기본 외부 호출 disabled |
| Shares Outstanding Import | 완료 | 발행주식수 CSV import API/history와 optional valuation generate 연동 구현. DART 사업보고서/합법 provider 자동 수집은 후속 작업으로 분리하고 자동 주문은 연결하지 않음 |
| Disclosure Evidence Provider | 완료 | `catalyst_evidences`와 disclosure import history/API 구현. 수동 evidence, DART/KRX/provider metadata evidence port, earnings event/preview/post review 자동 evidence, Morning Note evidence action item 추가. 뉴스 크롤링과 공시 원문 전체 저장, 자동 주문은 없음 |

## 4. 구현 단계

### Phase 1. 아키텍처 경계 정리

목표: application 계층에서 JPA 타입 의존을 제거한다.

- `StockPort` 출력 포트와 persistence adapter 추가
- 관심종목 조회 inbound port 추가
- Controller 응답을 `StockEntity`가 아닌 DTO/domain projection으로 변경
- 일봉 저장/조회 port 정의
- 지표 저장/조회 port 정의
- 알림 port 정의 후 `NotificationAdapter` 연결

완료 조건:

- `application` 패키지가 `adapter` 패키지를 import하지 않는다.
- ArchUnit 또는 패키지 의존성 테스트가 규칙을 검증한다.

### Phase 2. 시장 데이터 파이프라인

목표: 재현 가능한 분석 입력을 만든다.

- 일봉 upsert 유스케이스 구현
- 종목/기간별 일봉 조회 구현
- `(stock_code, trade_date)` 중복 정책 검증
- CSV fixture 또는 Fake Market Data Adapter 제공
- 데이터 정렬, 결측 거래일, 잘못된 OHLCV validation 추가
- 향후 KIS 시세 조회를 위한 `MarketDataPort` 정의

완료 조건:

- API 또는 테스트 fixture로 60개 이상의 일봉을 적재할 수 있다.
- 같은 일봉을 재적재해도 중복 행이 생성되지 않는다.
- 외부 API 없이 전체 분석 테스트를 재현할 수 있다.

### Phase 3. 지표와 전략 실행 유스케이스

목표: 저장된 일봉에서 후보 신호를 생성한다.

- 지표 계산 application service 구현
- `IndicatorSnapshot`과 Entity 양방향 매핑 구현
- 종목/거래일 기준 지표 중복 방지
- 전략 실행 inbound port와 service 구현
- 활성 관심종목 전체를 평가하는 수동 실행 endpoint 또는 scheduler 구현
- 데이터 부족 시 명확한 skip 결과 저장

완료 조건:

- 특정 종목과 날짜를 입력해 동일한 지표와 신호를 반복 생성할 수 있다.
- 점수와 reason이 DB에 저장되고 조회된다.
- 전략은 broker port를 직접 호출하지 않는다.

### Phase 4. 안전한 모의 주문 흐름

목표: 리스크 승인부터 모의 주문 저장까지 일관되게 처리한다.

- 주문 요청 REST API 또는 전략 실행 orchestration 연결
- 리스크 승인/거절 결과 DTO 정의
- 거절 시 예외 대신 판정 결과를 반환하고 이력 보존
- 신호 Entity 식별자를 유지해 상태를 update하도록 변경
- 주문 중복 복합 unique constraint 추가
- DB 중복 예외를 도메인 거절 결과로 변환
- Broker 실패 시 주문 상태와 실패 사유/시각/retryable 기록
- 주문 application integration test 추가

완료 조건:

- 승인 요청은 신호와 주문 이력이 각각 한 건으로 남는다.
- 거절 요청은 broker를 호출하지 않고 거절 사유가 남는다.
- 동시에 같은 주문을 요청해도 한 건만 접수된다.

### Phase 5. 조회, 알림, 운영 준비

목표: 모의 실행 결과를 사람이 검증할 수 있게 한다.

- 거래일/종목/상태별 신호 조회 API
- 주문 요청과 상태 조회 API
- 고득점 후보 및 리스크 거절 알림
- actuator health와 DB 상태 확인
- 비밀정보 마스킹과 구조화 로그
- Flyway migration 도입
- MySQL Testcontainers 통합 테스트
- API 예외 응답과 validation 규격 통일

완료 조건:

- 한 거래일의 분석, 판정, 주문 결과를 API와 로그로 추적할 수 있다.
- 새 DB에 migration을 적용해 애플리케이션을 시작할 수 있다.
- CI에서 unit/integration test가 통과한다.

## 5. MVP 이후

다음 항목은 MVP 1차 범위에 포함하지 않는다.

- 시장가 주문
- 자동매수
- 주문 정정과 장기 미체결 고도화
- 실시간 체결/호가 스트리밍
- 다중 전략 포트폴리오 최적화
- 과거 원천 데이터 기반 replay 백테스트 엔진
- 운영자용 Web UI
- MSA, Kafka, Kubernetes

실매매 1단계 이후에도 데이터 품질, 전략 재현성, 리스크 이력과 주문 안전성을 우선한다.
장초반 일별/기간 리포트도 저장 데이터 검증 전용이며 자동 주문 실행 경로와 연결하지 않는다.

## 6. 테스트 계획

| 계층 | 테스트 범위 |
| --- | --- |
| Domain | 지표 수식, 전략 점수 경계, 리스크 정책, 상태 전이 |
| Application | 승인/거절 흐름, broker 호출 여부, 이력 저장, 실패 처리 |
| Persistence | 매핑, unique constraint, 기간 조회, enum 저장 |
| Web | validation, 상태 코드, 오류 응답, JSON 계약 |
| Integration | H2 빠른 테스트와 MySQL Testcontainers 호환성 |

매 단계는 `./gradlew test` 통과와 관련 문서 업데이트를 완료 조건으로 한다.

## 7. 다음 구현 순서

가장 가까운 작업 순서는 다음과 같다.

KIS OAuth tokenP는 실전/모의 환경별 MEMORY cache, 만료 전 갱신,
KST 일별 갱신, scheduler, health/API/metrics와 AES-256-GCM DB cache까지
구현했다. 운영 환경은 외부 secret manager와 다중 인스턴스가 없는 단일
로컬 실행을 기준으로 한다.

1. market index provider와 sector master seed/import 방식 결정
2. consensus provider 범위와 합법적인 라이선스/입력 경로 결정
3. disclosure evidence DART/KRX 실제 provider adapter와 rate limit 정책 구현
4. shares outstanding provider 자동 수집 범위와 DART 사업보고서 parsing 정책 결정
5. indicator warmup 실패율과 종목별 일봉 누락 운영 점검
6. 장초반 원천 데이터 축적량과 누락률 운영 모니터링
7. 충분한 기간이 축적된 뒤 저장 원천 데이터 기반 replay 백테스트 구현
8. scheduler 실행 이력 보관 기간과 자동 정리 정책 결정
9. KIS 주문 정정 API와 취소가능조회 모의환경 공식 TR ID 확인
10. 로컬 encryption key 파일 권한과 백업·복구 절차 점검
