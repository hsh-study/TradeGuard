package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewCandidate;
import seokhoon.trade.application.port.in.ClosingBetFinalReviewResult;
import seokhoon.trade.application.port.in.ReviewClosingBetCandidatesUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.application.port.out.TradingSignalPort;
import seokhoon.trade.application.port.out.TradingSignalQueryPort;
import seokhoon.trade.application.port.out.TradingSignalRecord;
import seokhoon.trade.domain.strategy.ClosingBetStrategy;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignal;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ClosingBetFinalReviewService implements ReviewClosingBetCandidatesUseCase {
    private static final int MIN_PRE_SCAN_SCORE = 70;
    private static final int MIN_FINAL_SCORE = 75;

    private final TradingSignalQueryPort tradingSignalQueryPort;
    private final TradingSignalPort tradingSignalPort;
    private final NotificationPort notificationPort;
    private final Clock clock;

    @Autowired
    public ClosingBetFinalReviewService(
            TradingSignalQueryPort tradingSignalQueryPort,
            TradingSignalPort tradingSignalPort,
            NotificationPort notificationPort
    ) {
        this(tradingSignalQueryPort, tradingSignalPort, notificationPort, Clock.systemUTC());
    }

    ClosingBetFinalReviewService(
            TradingSignalQueryPort tradingSignalQueryPort,
            TradingSignalPort tradingSignalPort,
            NotificationPort notificationPort,
            Clock clock
    ) {
        this.tradingSignalQueryPort = tradingSignalQueryPort;
        this.tradingSignalPort = tradingSignalPort;
        this.notificationPort = notificationPort;
        this.clock = clock;
    }

    @Override
    public ClosingBetFinalReviewResult review(LocalDate tradeDate, int limit) {
        Objects.requireNonNull(tradeDate, "tradeDate");
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }

        List<TradingSignalRecord> preScanCandidates = tradingSignalQueryPort.find(new TradingSignalSearchCriteria(
                null,
                tradeDate,
                ClosingBetCandidateScanner.STRATEGY_NAME,
                SignalType.BUY_CANDIDATE,
                null,
                MIN_PRE_SCAN_SCORE
        ));
        List<FinalReviewSelection> selections = preScanCandidates.stream()
                .filter(signal -> signal.riskReasons().isEmpty())
                .map(this::review)
                .filter(selection -> selection.finalScore() >= MIN_FINAL_SCORE)
                .sorted(Comparator.comparingInt(FinalReviewSelection::finalScore).reversed()
                        .thenComparing(selection -> selection.preScanSignal().stockCode()))
                .limit(limit)
                .toList();

        selections.forEach(selection -> tradingSignalPort.save(new TradingSignal(
                ClosingBetStrategy.STRATEGY_NAME,
                selection.preScanSignal().stockCode(),
                tradeDate,
                SignalType.BUY_CANDIDATE,
                selection.finalScore(),
                selection.reasons()
        )));

        List<ClosingBetFinalReviewCandidate> selectedCandidates = restoreSavedCandidates(tradeDate, selections);
        NotificationDeliveryResult deliveryResult = sendBriefing(tradeDate, selectedCandidates);
        String summary = selectedCandidates.isEmpty()
                ? "15:00 최종 후보 없음"
                : "15:00 최종 후보 " + selectedCandidates.size() + "개";
        return new ClosingBetFinalReviewResult(
                tradeDate,
                preScanCandidates.size(),
                selectedCandidates.size(),
                deliveryResult.sent(),
                summary,
                selectedCandidates
        );
    }

    private FinalReviewSelection review(TradingSignalRecord preScanSignal) {
        List<String> reasons = new ArrayList<>();
        reasons.add("FINAL_REVIEW_15_00");
        reasons.add("PRE_SCAN_CONFIRMED");
        reasons.addAll(preScanSignal.reasons());
        // TODO: Replace this structural score with real 15:00 intraday snapshots when available.
        return new FinalReviewSelection(preScanSignal, preScanSignal.score(), reasons);
    }

    private List<ClosingBetFinalReviewCandidate> restoreSavedCandidates(
            LocalDate tradeDate,
            List<FinalReviewSelection> selections
    ) {
        Set<String> selectedCodes = selections.stream()
                .map(selection -> selection.preScanSignal().stockCode())
                .collect(java.util.stream.Collectors.toSet());
        Map<String, TradingSignalRecord> finalSignalsByCode = tradingSignalQueryPort.find(new TradingSignalSearchCriteria(
                        null,
                        tradeDate,
                        ClosingBetStrategy.STRATEGY_NAME,
                        SignalType.BUY_CANDIDATE,
                        null,
                        MIN_FINAL_SCORE
                ))
                .stream()
                .filter(record -> selectedCodes.contains(record.stockCode()))
                .collect(java.util.stream.Collectors.toMap(
                        TradingSignalRecord::stockCode,
                        record -> record,
                        (left, right) -> left
                ));
        return selections.stream()
                .map(selection -> {
                    TradingSignalRecord preScanSignal = selection.preScanSignal();
                    TradingSignalRecord finalSignal = finalSignalsByCode.get(preScanSignal.stockCode());
                    if (finalSignal == null) {
                        return new ClosingBetFinalReviewCandidate(
                                preScanSignal.id(),
                                null,
                                ClosingBetStrategy.STRATEGY_NAME,
                                preScanSignal.stockCode(),
                                selection.finalScore(),
                                selection.reasons(),
                                List.of(),
                                TradingSignalStatus.CREATED
                        );
                    }
                    return new ClosingBetFinalReviewCandidate(
                            preScanSignal.id(),
                            finalSignal.id(),
                            finalSignal.strategyName(),
                            finalSignal.stockCode(),
                            finalSignal.score(),
                            finalSignal.reasons(),
                            finalSignal.riskReasons(),
                            finalSignal.status()
                    );
                })
                .toList();
    }

    private NotificationDeliveryResult sendBriefing(
            LocalDate tradeDate,
            List<ClosingBetFinalReviewCandidate> candidates
    ) {
        StringBuilder body = new StringBuilder();
        body.append("tradeDate: ").append(tradeDate).append('\n');
        body.append("15:00 최종 종가베팅 후보 리뷰 결과입니다. 주문 요청은 생성하지 않습니다.\n\n");
        if (candidates.isEmpty()) {
            body.append("- 최종 후보 없음\n");
        } else {
            candidates.forEach(candidate -> body.append("- preScanSignalId=")
                    .append(candidate.preScanSignalId())
                    .append(", finalSignalId=")
                    .append(candidate.finalSignalId())
                    .append(", stockCode=")
                    .append(candidate.stockCode())
                    .append(", score=")
                    .append(candidate.score())
                    .append(", reasons=")
                    .append(candidate.reasons())
                    .append('\n'));
        }
        try {
            return notificationPort.send(new NotificationMessage(
                    "TradeGuard 15:00 종가베팅 최종 후보 - " + tradeDate,
                    body.toString(),
                    clock.instant()
            ));
        } catch (RuntimeException exception) {
            return NotificationDeliveryResult.skipped("notification delivery failed");
        }
    }

    private record FinalReviewSelection(
            TradingSignalRecord preScanSignal,
            int finalScore,
            List<String> reasons
    ) {
    }
}
