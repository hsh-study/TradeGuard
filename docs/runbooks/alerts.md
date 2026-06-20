# TradeGuard alert runbook

These alerts are prompts for manual investigation. They do not authorize automatic orders, retries, flag changes, kill-switch changes, or any other state mutation.

## Safety rules

- Never print, paste, or attach API keys, app secrets, access/refresh token values, account identifiers, or webhook URLs.
- Use status and metadata endpoints only; do not copy token values into tickets, chat, logs, or alert annotations.
- Do not run an automatic buy or sell because an alert fired.
- Do not automatically clear `LiveTradingReadinessBlocked`, enable live-trading flags, or disable the kill switch.
- Treat `increase(...)` alerts as evidence of a recent event. Confirm current state in the readiness API or operational dashboard before deciding what to do.

Start every investigation with:

```sh
curl 'http://localhost:8080/api/operations/dashboard'
curl 'http://localhost:8080/actuator/health'
```

## TradeGuardSchedulerFailureIncreased

1. Check `/api/operations/dashboard`.
2. Query `/api/scheduler-executions?status=FAILED` and identify the bounded `schedulerName` and failure time.
3. Correlate the execution with application logs without adding correlation IDs to metric or alert labels.
4. Check its upstream market data, database, calendar, or provider dependency.
5. Decide whether a documented manual rerun is safe. Do not retry automatically and do not use an order endpoint as a recovery action.

## TradeGuardKisReadOnlyFailureIncreased

1. Check the KIS service status and the application health/readiness endpoints.
2. Check `/api/kis/token/status` for token metadata; never expose the token value.
3. Review rate-limit responses and recent read-only operation failures.
4. Verify network and DNS reachability from TradeGuard.
5. Confirm that configured credentials exist without printing their values.

## TradeGuardKisTokenIssueFailure

1. Check `/api/kis/token/status`; do not include token values in the incident record.
2. Check KIS service availability, network connectivity, and issuance rate limits.
3. Confirm whether credentials were recently rotated and whether the running process received the updated secret configuration.
4. Review token issuance errors after redaction. Do not invoke refresh repeatedly or automate refresh from the alert.

## TradeGuardInvestorFlowNotReady

1. Check `/api/research/investor-flows/readiness` for the authoritative current report.
2. Review recent investor-flow import histories in `/api/research/investor-flows/import-histories`.
3. Confirm `KIS_INVESTOR_FLOW_AMOUNT_UNIT` is explicitly configured and verified for the provider data.
4. Check provider enablement, mapping verification, recent data coverage, and KIS read-only failures.

## TradeGuardDartFinancialImportFailure

1. Check the operational dashboard and recent DART import history/logs.
2. Confirm `DART_API_KEY` is configured without printing it.
3. Verify stock-to-corporation mapping coverage and the requested reporting period.
4. Confirm the DART provider is enabled and OpenDART is reachable.
5. Review rate-limit or schema errors with secret and source details redacted.

## TradeGuardDisclosureActualImportFailure

1. Check OpenDART availability and recent disclosure actual import history.
2. Verify corporation mapping and provider enablement.
3. Check rate limits, lookback configuration, and response mapping failures.
4. Keep receipt numbers, source URLs, titles, and credentials out of alert labels and incident notifications.

## TradeGuardConsensusImportFailure

1. Check which bounded consensus `type` failed in the dashboard.
2. Validate CSV encoding, header names, required columns, date formats, and numeric fields.
3. Compare the input against the documented earnings or target-price CSV contract.
4. Do not attach confidential input rows to broadly visible incident notifications.

## TradeGuardPaperTradingReportFailure

1. Check the operational dashboard and the requested report period.
2. Verify `daily_prices`, intraday bar archive data, and `trading_signals` coverage for that period.
3. Check whether a data-insufficient result was incorrectly interpreted as a failure.
4. Review persistence and calculation errors. Do not submit an order as a validation step.

## TradeGuardReplayBacktestFailure

1. Check the operational dashboard and failed backtest run metadata.
2. Verify stored `trading_signals`, `daily_prices`, and required intraday archive coverage.
3. Confirm strategy parameters and date range are valid.
4. Review persistence and calculation errors. A rerun must remain a backtest and must never invoke a broker order.

## TradeGuardLiveTradingReadinessBlocked

1. Check `/api/live-trading/readiness` for the authoritative current report.
2. Review the kill switch and the configured values of `LIVE_TRADING_ENABLED` and `KIS_TRADING_ENABLED` without changing them.
3. Check KIS trading configuration, account metadata presence, market session requirements, and other readiness reasons without exposing account or credential values.
4. Keep the system blocked until an operator separately verifies and deliberately resolves every reason under the live-trading operating procedure.
5. Never auto-enable flags, clear the kill switch, or submit an order from this alert.
