package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetBriefingResult;
import seokhoon.trade.application.port.in.TradingSignalView;
import seokhoon.trade.application.port.out.NotificationDeliveryResult;
import seokhoon.trade.application.port.out.NotificationMessage;
import seokhoon.trade.application.port.out.NotificationPort;
import seokhoon.trade.domain.strategy.SignalType;
import seokhoon.trade.domain.strategy.TradingSignalStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClosingBetBriefingServiceTest {
    private static final LocalDate SIGNAL_DATE = LocalDate.of(2026, 6, 5);

    @Test
    void includesHighScoreClosingBetCandidatesInMessage() {
        RecordingNotificationPort notificationPort = new RecordingNotificationPort();
        ClosingBetBriefingService service = service(
                List.of(signal(1L, "005930", 80, List.of("MA5_ABOVE_MA20"), List.of())),
                notificationPort
        );

        ClosingBetBriefingResult result = service.send(SIGNAL_DATE);

        assertThat(result.sent()).isTrue();
        assertThat(result.candidateCount()).isEqualTo(1);
        assertThat(result.messageBody())
                .contains("## 고득점 후보(score >= 70)")
                .contains("signalId=1")
                .contains("stockCode=005930")
                .contains("score=80")
                .contains("MA5_ABOVE_MA20");
        assertThat(notificationPort.messages)
                .singleElement()
                .satisfies(message -> assertThat(message.title()).contains("종가베팅 브리핑"));
    }

    @Test
    void separatesSignalsWithRiskReasonsIntoRiskSection() {
        ClosingBetBriefingService service = service(
                List.of(signal(2L, "000660", 90, List.of("TEST"), List.of("DUPLICATE_ORDER"))),
                new RecordingNotificationPort()
        );

        ClosingBetBriefingResult result = service.send(SIGNAL_DATE);

        assertThat(result.candidateCount()).isZero();
        assertThat(result.riskCandidateCount()).isEqualTo(1);
        assertThat(result.messageBody())
                .contains("## 리스크 후보")
                .contains("signalId=2")
                .contains("stockCode=000660")
                .contains("riskReasons=[DUPLICATE_ORDER]");
    }

    private static ClosingBetBriefingService service(
            List<TradingSignalView> signals,
            NotificationPort notificationPort
    ) {
        return new ClosingBetBriefingService(
                criteria -> {
                    assertThat(criteria.signalDate()).isEqualTo(SIGNAL_DATE);
                    assertThat(criteria.strategyName()).isEqualTo("CLOSING_BET");
                    return signals;
                },
                notificationPort,
                Clock.fixed(Instant.parse("2026-06-05T06:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static TradingSignalView signal(
            Long signalId,
            String stockCode,
            int score,
            List<String> reasons,
            List<String> riskReasons
    ) {
        return new TradingSignalView(
                signalId,
                "CLOSING_BET",
                stockCode,
                SIGNAL_DATE,
                SignalType.BUY_CANDIDATE,
                score,
                reasons,
                riskReasons,
                riskReasons.isEmpty() ? TradingSignalStatus.CREATED : TradingSignalStatus.RISK_REJECTED
        );
    }

    private static class RecordingNotificationPort implements NotificationPort {
        private final List<NotificationMessage> messages = new ArrayList<>();

        @Override
        public NotificationDeliveryResult send(NotificationMessage message) {
            messages.add(message);
            return NotificationDeliveryResult.success();
        }
    }
}
