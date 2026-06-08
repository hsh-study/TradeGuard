package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.ClosingBetBriefingResult;
import seokhoon.trade.application.port.in.LoadTradingSignalsUseCase;
import seokhoon.trade.application.port.in.SendClosingBetBriefingUseCase;
import seokhoon.trade.application.port.in.TradingSignalSearchCriteria;
import seokhoon.trade.application.port.in.TradingSignalView;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.domain.strategy.ClosingBetStrategy;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ClosingBetBriefingService implements SendClosingBetBriefingUseCase {
    private static final int MINIMUM_BRIEFING_SCORE = 70;

    private final LoadTradingSignalsUseCase loadTradingSignalsUseCase;
    private final NotificationPort notificationPort;
    private final Clock clock;

    @Autowired
    public ClosingBetBriefingService(
            LoadTradingSignalsUseCase loadTradingSignalsUseCase,
            NotificationPort notificationPort
    ) {
        this(loadTradingSignalsUseCase, notificationPort, Clock.systemUTC());
    }

    ClosingBetBriefingService(
            LoadTradingSignalsUseCase loadTradingSignalsUseCase,
            NotificationPort notificationPort,
            Clock clock
    ) {
        this.loadTradingSignalsUseCase = loadTradingSignalsUseCase;
        this.notificationPort = notificationPort;
        this.clock = clock;
    }

    @Override
    public ClosingBetBriefingResult send(LocalDate signalDate) {
        Objects.requireNonNull(signalDate, "signalDate");
        List<TradingSignalView> signals = loadTradingSignalsUseCase.load(new TradingSignalSearchCriteria(
                null,
                signalDate,
                ClosingBetStrategy.STRATEGY_NAME,
                null,
                null,
                null
        ));
        List<TradingSignalView> candidates = signals.stream()
                .filter(signal -> signal.score() >= MINIMUM_BRIEFING_SCORE)
                .filter(signal -> signal.riskReasons().isEmpty())
                .sorted(Comparator.comparingInt(TradingSignalView::score).reversed())
                .toList();
        List<TradingSignalView> riskCandidates = signals.stream()
                .filter(signal -> !signal.riskReasons().isEmpty())
                .sorted(Comparator.comparingInt(TradingSignalView::score).reversed())
                .toList();

        String summary = "종가베팅 후보 " + candidates.size() + "개, 리스크 후보 " + riskCandidates.size() + "개";
        String body = buildBody(signalDate, candidates, riskCandidates);
        NotificationMessage message = new NotificationMessage(
                "TradeGuard 종가베팅 브리핑 - " + signalDate,
                body,
                clock.instant()
        );

        NotificationDeliveryResult deliveryResult;
        try {
            deliveryResult = notificationPort.send(message);
        } catch (RuntimeException exception) {
            deliveryResult = NotificationDeliveryResult.skipped("notification delivery failed");
        }
        return new ClosingBetBriefingResult(
                deliveryResult.sent(),
                candidates.size(),
                riskCandidates.size(),
                summary,
                body,
                signalDate
        );
    }

    private static String buildBody(
            LocalDate signalDate,
            List<TradingSignalView> candidates,
            List<TradingSignalView> riskCandidates
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("signalDate: ").append(signalDate).append('\n');
        builder.append("실계좌 주문 없음. 시장가 주문 없음. 알림 전용 브리핑입니다.\n\n");
        builder.append("## 고득점 후보(score >= 70)\n");
        if (candidates.isEmpty()) {
            builder.append("- 없음\n");
        } else {
            candidates.forEach(signal -> appendSignal(builder, signal, false));
        }
        builder.append("\n## 리스크 후보\n");
        if (riskCandidates.isEmpty()) {
            builder.append("- 없음\n");
        } else {
            riskCandidates.forEach(signal -> appendSignal(builder, signal, true));
        }
        return builder.toString();
    }

    private static void appendSignal(StringBuilder builder, TradingSignalView signal, boolean includeRiskReasons) {
        builder.append("- signalId=")
                .append(signal.signalId())
                .append(", stockCode=")
                .append(signal.stockCode())
                .append(", score=")
                .append(signal.score())
                .append(", status=")
                .append(signal.status())
                .append(", reasons=")
                .append(signal.reasons());
        if (includeRiskReasons) {
            builder.append(", riskReasons=").append(signal.riskReasons());
        }
        builder.append('\n');
    }
}
