# TradeGuard observability

TradeGuard exports Micrometer metrics for the Prometheus and Grafana containers that are already running locally. This repository does not create, replace, or mount those containers.

## Endpoints and local ports

- Spring operational state UI: `http://localhost:18080/operations/dashboard`
- Watchlist and stored market chart UI: `http://localhost:18080/operations/watchlist`
- Operational state API: `http://localhost:18080/api/operations/dashboard`
- Boot readiness API: `http://localhost:18080/api/operations/boot-readiness`
- TradeGuard metrics: `http://localhost:18080/actuator/prometheus`
- TradeGuard health: `http://localhost:18080/actuator/health`
- Existing Prometheus: `http://localhost:19090`
- Existing Grafana: `http://localhost:13000`

The application exposes `health`, `info`, `metrics`, and `prometheus`. Exported series include the common `application="tradeguard"` label.

## Grafana and Spring dashboard roles

Boot Readiness is the startup-configuration view: once after `ApplicationReadyEvent`, it checks local configuration and minimal DB/Flyway state without calling external providers. The Spring Operational Dashboard UI is the selected-date current-state view: it summarizes blocking issues, warnings, recommended actions, readiness, scheduler, research, and paper/backtest status. Grafana and Prometheus are the metric-trend view: they show how counters and failure signals changed over time. Alerting turns those recent metric events into prompts for manual investigation.

Start with `/api/operations/boot-readiness` to confirm configuration safety, use `/operations/dashboard` for current operational state, and use Grafana for timing and trends. These surfaces are read-only. They do not call external providers, refresh tokens, create orders, change flags, or clear the kill switch. They only include approved summary fields and omit credentials, account identifiers, webhook/source URLs, receipt numbers, provider details, raw scheduler failure reasons, and bulk stock-code lists.

`/operations/watchlist` has a different role: it manages the watchlist, can explicitly start KIS daily-price/material collection, reads stored OHLCV and indicators, and opens a read-only SSE current-price stream during regular market hours. It also contains operator-triggered, account-selected LIMIT buy/sell forms. Those forms are state-changing and remain behind confirmation, readiness, kill-switch, market-hours, and amount checks; they are not part of the read-only dashboard or Grafana surfaces.

## Connect the existing Prometheus container

1. Start TradeGuard with `./gradlew bootRun`.
2. Confirm `http://localhost:18080/actuator/prometheus` returns Prometheus text format.
3. Find the active configuration used by the existing `prometheus` container, for example with `docker inspect prometheus`. Do not assume that editing a host file changes the mounted file.
4. Merge the job from [`tradeguard-scrape-example.yml`](../observability/prometheus/tradeguard-scrape-example.yml) into the existing `prometheus.yml` `scrape_configs` list. Do not add a second top-level `scrape_configs` key.
5. Validate the effective Prometheus configuration, then reload Prometheus if lifecycle reload is enabled or restart only the existing container: `docker restart prometheus`.
6. Open `http://localhost:19090/targets` and confirm the `tradeguard` target is `UP`.

On the current local Docker network, prefer `tradeguard:8080` when Prometheus shares `study_default`. If Prometheus reaches the published host port, Docker Desktop commonly uses `host.docker.internal:18080`; a Linux host gateway may use port `18080`. Verify the address from inside the Prometheus container.

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
- `tradeguard.research.news_import.count` — `provider=naver`, `result=success|partial|failure|skipped`
- `tradeguard.research.news_classification.count` — bounded `category`, `sentiment`, `result=classified|unclassified`
- `tradeguard.research.paper_trading_report.count` — `result=success|failure|insufficient`
- `tradeguard.research.replay_backtest.count` — `strategy`, `result=success|failure|insufficient`
- `tradeguard.live_trading.readiness.count` — `result=ready|blocked`
- `tradeguard.operations.boot_readiness.count` — `result=ready|warning|blocked`
- `tradeguard.order.request.count` — `status`

Counters exist only after their event first occurs. A blank panel can therefore mean no event has yet registered since application startup, not a zero result. Dashboard panels show increases over the selected time range and filter on `application="tradeguard"`.

## Apply operational alert rules

The recommended source of truth is the Prometheus rule file [`tradeguard-alert-rules.example.yml`](../observability/prometheus/tradeguard-alert-rules.example.yml). It contains ten event-based operational alerts and no receiver or notification secret configuration.

1. Inspect the existing `prometheus` container to find its effective configuration file and mounted paths, for example with `docker inspect prometheus`.
2. Make the rule file available inside that existing container. Depending on the current deployment, add a read-only host bind mount through its existing launch configuration or copy the file as a temporary local setup. The operator must verify the actual persistent mount/configuration; this repository does not change it.
3. Add `rule_files` to the existing `prometheus.yml`, using the path as seen inside the container:

   ```yaml
   rule_files:
     - /etc/prometheus/tradeguard-alert-rules.example.yml
   ```

4. Validate the complete effective Prometheus configuration and rule syntax with the `promtool` version bundled with that Prometheus installation when available.
5. Reload configuration through the existing lifecycle endpoint only when it is already enabled, or restart only the existing container with `docker restart prometheus`.
6. Open `http://localhost:19090/rules` to confirm the group loaded and `http://localhost:19090/alerts` to inspect alert state.

Adding a rule file does not configure delivery. Connect an already approved notification path separately; never place a real Slack/Discord webhook, token, or account identifier in this repository. No Alertmanager container is added by this project.

### Grafana managed alert reference

[`tradeguard-alerts.example.json`](../observability/grafana/alerting/tradeguard-alerts.example.json) is a version-neutral reference, not a guaranteed one-click export for every Grafana version. Grafana managed-alert export/import schemas vary. Prefer the Prometheus rules above. If Grafana-managed alerts are required, open `http://localhost:13000`, select the existing Prometheus datasource, and manually create each rule from the reference expression and `last(query) > 0` condition. Replace the datasource UID placeholder locally; do not commit environment-specific identifiers or contact-point secrets.

All rules use counter `increase(...)` over a recent window. A firing alert proves that an undesirable event occurred in that window; it does not prove the condition is still current after recovery. The readiness APIs and `/api/operations/dashboard` are authoritative for current state. Follow the manual procedures in [`runbooks/alerts.md`](runbooks/alerts.md).

## Operations checklist

- TradeGuard `/actuator/health` is healthy and `/actuator/prometheus` is reachable.
- Boot readiness is available and any `BLOCKED` result has been reviewed before operation.
- Prometheus `tradeguard` target is `UP`; scrape errors and last scrape age are normal.
- Grafana datasource test succeeds and dashboard panels query the expected datasource.
- Scheduler and provider failures have not increased in the operating window.
- Investor flow and live trading readiness show no recent undesirable result; confirm current state through the corresponding API.
- Token refresh failures are investigated without logging token or credential values.
- Prometheus rules are loaded and visible at `http://localhost:19090/rules`; alert state is visible at `http://localhost:19090/alerts`.
- Alert responders follow [`runbooks/alerts.md`](runbooks/alerts.md) and confirm current readiness before acting.
- Metrics contain only approved bounded tags and no stock, account, secret, webhook, URL, receipt, title, or correlation identifiers.
- News metrics never include a query, stock code, title, link/source URL, publisher, or Naver credential.
- Observability and alerting remain read-only; they never enable flags, clear readiness blocks, or submit automatic orders.
