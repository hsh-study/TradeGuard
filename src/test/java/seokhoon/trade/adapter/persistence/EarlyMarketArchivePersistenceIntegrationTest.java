package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.EarlyMarketAfterHoursSnapshotPort;
import seokhoon.trade.application.port.out.EarlyMarketDataCapturePort;
import seokhoon.trade.application.port.out.EarlyMarketIntradayBarSnapshotPort;
import seokhoon.trade.application.port.out.EarlyMarketMarketSnapshotArchivePort;
import seokhoon.trade.application.port.out.EarlyMarketRankingSnapshotPort;
import seokhoon.trade.domain.market.BarInterval;
import seokhoon.trade.domain.market.EarlyMarketAfterHoursSnapshot;
import seokhoon.trade.domain.market.EarlyMarketCaptureStatus;
import seokhoon.trade.domain.market.EarlyMarketCaptureType;
import seokhoon.trade.domain.market.EarlyMarketDataCapture;
import seokhoon.trade.domain.market.EarlyMarketIntradayBarSnapshot;
import seokhoon.trade.domain.market.EarlyMarketMarketSnapshot;
import seokhoon.trade.domain.market.EarlyMarketRankingSnapshot;
import seokhoon.trade.domain.market.EarlyMarketSnapshotType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EarlyMarketArchivePersistenceIntegrationTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 6, 10);
    private static final Instant CAPTURED_AT =
            Instant.parse("2026-06-10T00:05:00Z");

    @Autowired
    private EarlyMarketDataCapturePort capturePort;
    @Autowired
    private EarlyMarketRankingSnapshotPort rankingPort;
    @Autowired
    private EarlyMarketAfterHoursSnapshotPort afterHoursPort;
    @Autowired
    private EarlyMarketIntradayBarSnapshotPort barPort;
    @Autowired
    private EarlyMarketMarketSnapshotArchivePort marketSnapshotPort;

    @Test
    void storesRankingAfterHoursAndMarketSnapshots() {
        rankingPort.saveAll(List.of(ranking()));
        afterHoursPort.upsertAfterHours(List.of(afterHours()));
        marketSnapshotPort.upsertMarketSnapshots(List.of(marketSnapshot()));

        assertThat(rankingPort.findRankings(TRADE_DATE))
                .singleElement()
                .extracting(EarlyMarketRankingSnapshot::stockCode)
                .isEqualTo("005930");
        assertThat(afterHoursPort.findAfterHours(TRADE_DATE))
                .singleElement()
                .extracting(EarlyMarketAfterHoursSnapshot::previousTradingDay)
                .isEqualTo(LocalDate.of(2026, 6, 9));
        assertThat(marketSnapshotPort.findMarketSnapshots(
                TRADE_DATE,
                "005930"
        )).singleElement()
                .extracting(EarlyMarketMarketSnapshot::snapshotType)
                .isEqualTo(EarlyMarketSnapshotType.OPENING_0905);
    }

    @Test
    void upsertsSameIntradayBarAndCaptureType() {
        barPort.upsertBars(List.of(bar(BigDecimal.valueOf(101))));
        barPort.upsertBars(List.of(bar(BigDecimal.valueOf(102))));
        capturePort.save(capture(EarlyMarketCaptureStatus.FAILED, 0));
        capturePort.save(capture(EarlyMarketCaptureStatus.SUCCEEDED, 1));

        assertThat(barPort.findBars(TRADE_DATE, "005930"))
                .singleElement()
                .extracting(EarlyMarketIntradayBarSnapshot::closePrice)
                .isEqualTo(BigDecimal.valueOf(102));
        assertThat(capturePort.findCaptures(TRADE_DATE))
                .singleElement()
                .satisfies(capture -> {
                    assertThat(capture.status())
                            .isEqualTo(EarlyMarketCaptureStatus.SUCCEEDED);
                    assertThat(capture.itemCount()).isEqualTo(1);
                });
    }

    private static EarlyMarketRankingSnapshot ranking() {
        return new EarlyMarketRankingSnapshot(
                null, TRADE_DATE, CAPTURED_AT, 1, "005930", "삼성전자",
                BigDecimal.valueOf(75000), BigDecimal.valueOf(2.1),
                1000, BigDecimal.valueOf(75_000_000),
                "MARKET_RANKING_PORT:KOSPI:TRADING_VALUE"
        );
    }

    private static EarlyMarketAfterHoursSnapshot afterHours() {
        return new EarlyMarketAfterHoursSnapshot(
                null, TRADE_DATE, LocalDate.of(2026, 6, 9), CAPTURED_AT,
                "005930", BigDecimal.valueOf(75100), BigDecimal.valueOf(0.2),
                100, BigDecimal.valueOf(7_510_000),
                "AFTER_HOURS_MARKET_DATA_PORT"
        );
    }

    private static EarlyMarketIntradayBarSnapshot bar(BigDecimal close) {
        return new EarlyMarketIntradayBarSnapshot(
                null, TRADE_DATE, "005930", CAPTURED_AT, LocalTime.of(9, 1),
                BarInterval.ONE_MINUTE, BigDecimal.valueOf(100),
                BigDecimal.valueOf(103), BigDecimal.valueOf(99), close,
                1000, BigDecimal.valueOf(101_000), BigDecimal.valueOf(101),
                "INTRADAY_BAR_PORT"
        );
    }

    private static EarlyMarketMarketSnapshot marketSnapshot() {
        return new EarlyMarketMarketSnapshot(
                null, TRADE_DATE, "005930", CAPTURED_AT,
                EarlyMarketSnapshotType.OPENING_0905, BigDecimal.valueOf(102),
                BigDecimal.valueOf(103), BigDecimal.valueOf(99), 1000,
                BigDecimal.valueOf(101_000), BigDecimal.valueOf(101),
                "MARKET_SNAPSHOT_PORT"
        );
    }

    private static EarlyMarketDataCapture capture(
            EarlyMarketCaptureStatus status,
            int count
    ) {
        return new EarlyMarketDataCapture(
                null, TRADE_DATE,
                EarlyMarketCaptureType.OPENING_BARS_0900_0930,
                CAPTURED_AT, "INTRADAY_BAR_PORT", status, count,
                status == EarlyMarketCaptureStatus.FAILED ? "FAILED" : null,
                CAPTURED_AT
        );
    }
}
