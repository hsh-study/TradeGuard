package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.domain.indicator.IndicatorSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndicatorSnapshotServiceTest {
    private final InMemoryIndicatorSnapshotPort snapshotPort = new InMemoryIndicatorSnapshotPort();
    private final IndicatorSnapshotService snapshotService = new IndicatorSnapshotService(snapshotPort);

    @Test
    void savesAndLoadsSnapshotsInDateOrder() {
        IndicatorSnapshot later = snapshot(LocalDate.of(2026, 6, 5), "71000");
        IndicatorSnapshot earlier = snapshot(LocalDate.of(2026, 6, 4), "70000");

        snapshotService.save(later);
        snapshotService.save(earlier);

        assertThat(snapshotService.load(
                "005930",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 7)
        )).extracting(IndicatorSnapshot::tradeDate)
                .containsExactly(earlier.tradeDate(), later.tradeDate());
    }

    @Test
    void rejectsNullSnapshot() {
        assertThatNullPointerException()
                .isThrownBy(() -> snapshotService.save(null))
                .withMessage("snapshot");
    }

    @Test
    void rejectsInvertedDateRange() {
        assertThatThrownBy(() -> snapshotService.load(
                "005930",
                LocalDate.of(2026, 6, 7),
                LocalDate.of(2026, 6, 1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must not be after to");
    }

    private static IndicatorSnapshot snapshot(LocalDate tradeDate, String ma5) {
        return new IndicatorSnapshot(
                "005930",
                tradeDate,
                new BigDecimal(ma5),
                new BigDecimal("69000"),
                new BigDecimal("65000"),
                new BigDecimal("55"),
                new BigDecimal("100"),
                new BigDecimal("80"),
                new BigDecimal("20"),
                new BigDecimal("75000"),
                new BigDecimal("69000"),
                new BigDecimal("63000")
        );
    }

    private static class InMemoryIndicatorSnapshotPort implements IndicatorSnapshotPort {
        private final List<IndicatorSnapshot> snapshots = new ArrayList<>();

        @Override
        public IndicatorSnapshot save(IndicatorSnapshot snapshot) {
            snapshots.removeIf(saved ->
                    saved.stockCode().equals(snapshot.stockCode())
                            && saved.tradeDate().equals(snapshot.tradeDate())
            );
            snapshots.add(snapshot);
            return snapshot;
        }

        @Override
        public List<IndicatorSnapshot> findByStockCodeAndTradeDateBetween(
                String stockCode,
                LocalDate from,
                LocalDate to
        ) {
            return snapshots.stream()
                    .filter(snapshot -> snapshot.stockCode().equals(stockCode))
                    .filter(snapshot -> !snapshot.tradeDate().isBefore(from) && !snapshot.tradeDate().isAfter(to))
                    .sorted(Comparator.comparing(IndicatorSnapshot::tradeDate))
                    .toList();
        }
    }
}
