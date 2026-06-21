# Multi-agent analysis design

`NewsSignalAgent`, `CatalystAgent`, `RiskSignalAgent`, and `MorningNoteSummaryAgent` may consume
stored news metadata. Their output contract is limited to `signal`, `confidence`, `reasoning`,
`warnings`, and `suggestedManualReview`.

Agent output cannot contain or invoke an order command, quantity, limit price, readiness override,
kill-switch change, or auto-trading flag change. No agent is wired to `BrokerPort` or an order
application service. Deterministic keyword classification remains v1; LLM classification is a v2
candidate requiring a separately reviewed, read-only adapter.
