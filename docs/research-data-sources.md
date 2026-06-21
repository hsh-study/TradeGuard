# Research data sources

| Source | Allowed use | Prohibited use |
|---|---|---|
| KIS Open API | price, bars, account read, explicitly confirmed limit orders | credential exposure, unconfirmed real orders |
| OpenDART | disclosure and financial evidence | treating an unverified mapping as confirmed evidence |
| KRX/configured calendar | trading-day and market reference data | readiness override |
| Naver News Search API | title/summary/link metadata, deterministic classification, operator review | crawling, full article storage, news-only trading |
| CSV/manual consensus | traceable research snapshot | presenting stale/manual data as live provider data |

Naver news credentials remain in environment configuration. Search results are deduplicated by
source URL hash and normalized title hash. Stored summaries are API-provided snippets, not article
bodies. Disclosure evidence has priority when news and a disclosure describe the same event.
