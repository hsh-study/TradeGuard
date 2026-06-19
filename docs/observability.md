# TradeGuard observability

TradeGuard exports Micrometer metrics for the Prometheus and Grafana containers that are already running locally. This repository does not create, replace, or mount those containers.

## Endpoints and local ports

- TradeGuard metrics: `http://localhost:8080/actuator/prometheus`
- TradeGuard health: `http://localhost:8080/actuator/health`
- Existing Prometheus: `http://localhost:19090`
- Existing Grafana: `http://localhost:13000`

The application exposes `health`, `info`, `metrics`, and `prometheus`. Exported series include the common `application="tradeguard"` label.

## Connect the existing Prometheus container

1. Start TradeGuard with `./gradlew bootRun`.
2. Confirm `http://localhost:8080/actuator/prometheus` returns Prometheus text format.
3. Find the active configuration used by the existing `prometheus` container, for example with `docker inspect prometheus`. Do not assume that editing a host file changes the mounted file.
4. Merge the job from [`tradeguard-scrape-example.yml`](../observability/prometheus/tradeguard-scrape-example.yml) into the existing `prometheus.yml` `scrape_configs` list. Do not add a second top-level `scrape_configs` key.
5. Validate the effective Prometheus configuration, then reload Prometheus if lifecycle reload is enabled or restart only the existing container: `docker restart prometheus`.
6. Open `http://localhost:19090/targets` and confirm the `tradeguard` target is `UP`.

On Docker Desktop the application host is normally `host.docker.internal:8080`. A common Linux Docker bridge gateway is `172.17.0.1:8080`; use the address reachable from inside the existing container. A host firewall or a server bound only to an unreachable interface can also keep the target down.

## Import the dashboard into existing Grafana

1. Open `http://localhost:13000`.
2. Confirm a Prometheus datasource exists. When Grafana and Prometheus share a Docker network, its URL is commonly `http://prometheus:9090`. If Grafana reaches the published host port instead, use `http://host.docker.internal:19090` (or the Linux host gateway equivalent).
3. Choose **Dashboards > New > Import** and upload [`tradeguard-operational-dashboard.json`](../observability/grafana/dashboards/tradeguard-operational-dashboard.json).
4. Select the existing Prometheus datasource when prompted and import.

The optional [`prometheus.example.yml`](../observability/grafana/provisioning/datasources/prometheus.example.yml) is a reference only. No container mount or provisioning change is performed by this repository.

## Metric naming and tags

Java code uses dot-separated Micrometer names such as `tradeguard.scheduler.execution.count`. Prometheus normalizes these to snake case and appends the counter suffix, for example `tradeguard_scheduler_execution_count_total`. All custom names start with `tradeguard.` and represent monotonically increasing counters.

Allowed tags are bounded operational dimensions: `status`, `result`, `schedulerName`, `operation`, `environment`, `cacheMode`, `retryable`, `sent`, `scope`, `type`, `provider`, `strategy`, `side`, `market`, `year`, `captureType`, `decision`, `confidence`, `importance`, and `thesisImpact`. Spring adds the fixed common tag `application=tradeguard`.

Forbidden tags are secrets, personal/account identifiers, high-cardinality request or content data, and security-sensitive locations. In particular, never tag `stockCode`, account or account number, app key, app secret, access/refresh token, webhook URL, `correlationId`, `requestCorrelationId`, `sourceUrl`, `receiptNo`, `providerName`, or `title`. These values must not appear in Actuator details either.

## Dashboard metrics

The dashboard uses these current counters:

- `tradeguard.scheduler.execution.count` — `schedulerName`, `status`
- `tradeguard.kis.read_only.count` — `operation`, `result`
- `tradeguard.kis.token.cache.count` — `environment`, `result`; `result=refresh` is a refresh attempt
- `tradeguard.kis.token.issue.count` — `environment`, `result`; refresh success/failure outcome
- `tradeguard.indicator.warmup.count` — `result=success|partial|failure|skipped`
- `tradeguard.research.investor_flow_import.count` — `scope`, `result=success|partial|failure|skipped`
- `tradeguard.research.investor_flow_readiness.count` — `result=ready|not_ready`
- `tradeguard.research.supply_demand_analysis.count` — `result=success|insufficient|failure|blocked`
- `tradeguard.research.dart_financial_import.count` — `result=success|partial|failure|skipped`
- `tradeguard.research.disclosure_actual_import.count` — `provider`, `result=success|partial|failure|skipped`
- `tradeguard.research.consensus_import.count` — `type`, `result=success|failure`
- `tradeguard.research.paper_trading_report.count` — `result=success|failure|insufficient`
- `tradeguard.research.replay_backtest.count` — `strategy`, `result=success|failure|insufficient`
- `tradeguard.live_trading.readiness.count` — `result=ready|blocked`
- `tradeguard.order.request.count` — `status`

Counters exist only after their event first occurs. A blank panel can therefore mean no event has yet registered since application startup, not a zero result. Dashboard panels show increases over the selected time range and filter on `application="tradeguard"`.

## Alert candidates

- Scheduler `FAILED` count increases.
- KIS read-only API `failure` count increases.
- KIS token issue `failure` count increases after a refresh attempt.
- Investor flow readiness records `not_ready`.
- DART financial or disclosure actual import failure increases.
- Consensus import failure increases.
- Paper trading report or replay backtest failure increases.
- Live trading readiness records `blocked` (the current metric value corresponding to false/not ready).

For readiness counters, alert on a recent undesirable event only with an agreed evaluation window. A counter cannot by itself prove the current state after a later recovery; use the readiness API/health endpoint for authoritative current-state checks.

## Operations checklist

- TradeGuard `/actuator/health` is healthy and `/actuator/prometheus` is reachable.
- Prometheus `tradeguard` target is `UP`; scrape errors and last scrape age are normal.
- Grafana datasource test succeeds and dashboard panels query the expected datasource.
- Scheduler and provider failures have not increased in the operating window.
- Investor flow and live trading readiness show no recent undesirable result; confirm current state through the corresponding API.
- Token refresh failures are investigated without logging token or credential values.
- Metrics contain only approved bounded tags and no stock, account, secret, webhook, URL, receipt, title, or correlation identifiers.
- Observability remains read-only; it never enables or submits automatic orders.
