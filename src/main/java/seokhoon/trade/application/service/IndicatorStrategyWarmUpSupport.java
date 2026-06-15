package seokhoon.trade.application.service;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.WarmUpDailyPricesAndIndicatorsUseCase;
import seokhoon.trade.application.port.out.IndicatorSnapshotPort;
import seokhoon.trade.config.IndicatorWarmUpProperties;
import seokhoon.trade.domain.indicator.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class IndicatorStrategyWarmUpSupport {
    private final WarmUpDailyPricesAndIndicatorsUseCase warmUpUseCase;
    private final IndicatorSnapshotPort snapshotPort;
    private final IndicatorWarmUpProperties properties;

    public IndicatorStrategyWarmUpSupport(
            WarmUpDailyPricesAndIndicatorsUseCase warmUpUseCase,
            IndicatorSnapshotPort snapshotPort,
            IndicatorWarmUpProperties properties
    ) {
        this.warmUpUseCase = warmUpUseCase;
        this.snapshotPort = snapshotPort;
        this.properties = properties;
    }

    private IndicatorStrategyWarmUpSupport() {
        this.warmUpUseCase = null;
        this.snapshotPort = null;
        this.properties = new IndicatorWarmUpProperties();
        this.properties.setEnabled(false);
    }

    static IndicatorStrategyWarmUpSupport disabled() {
        return new IndicatorStrategyWarmUpSupport();
    }

    Session prepare(Collection<String> stockCodes, LocalDate date) {
        List<String> codes = stockCodes.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (warmUpUseCase == null) {
            return new Session(Map.of(), date, null, properties);
        }
        Map<String, IndicatorWarmUpResult> results = new HashMap<>();
        try {
            warmUpUseCase.warmUpStocks(codes, date)
                    .forEach(result ->
                            results.put(result.stockCode(), result));
        } catch (RuntimeException ignored) {
            // Strategy execution continues with explicit insufficiency.
        }
        codes.stream()
                .filter(code -> !results.containsKey(code))
                .forEach(code -> results.put(code, insufficient(code, date)));
        return new Session(results, date, snapshotPort, properties);
    }

    private static IndicatorWarmUpResult insufficient(
            String stockCode,
            LocalDate baseDate
    ) {
        return new IndicatorWarmUpResult(
                stockCode, baseDate, null, null, 0, 0,
                false, false, false,
                List.of("INDICATOR_DATA_INSUFFICIENT"),
                IndicatorWarmUpStatus.FAILED
        );
    }

    static final class Session {
        private final Map<String, IndicatorWarmUpResult> results;
        private final LocalDate baseDate;
        private final IndicatorSnapshotPort snapshotPort;
        private final IndicatorWarmUpProperties properties;

        private Session(
                Map<String, IndicatorWarmUpResult> results,
                LocalDate baseDate,
                IndicatorSnapshotPort snapshotPort,
                IndicatorWarmUpProperties properties
        ) {
            this.results = Map.copyOf(results);
            this.baseDate = baseDate;
            this.snapshotPort = snapshotPort;
            this.properties = properties;
        }

        Assessment assess(String stockCode, BigDecimal currentPrice) {
            IndicatorWarmUpResult result = results.get(stockCode);
            List<String> reasons = new ArrayList<>();
            if (result != null && !result.sufficientForMa60()) {
                reasons.add("INDICATOR_DATA_INSUFFICIENT");
            }
            boolean excluded = result != null
                    && !result.sufficientForMa60()
                    && properties.isFailStrategyWhenInsufficient();
            int adjustment = 0;
            Optional<IndicatorSnapshot> indicator = latest(stockCode);
            if (indicator.isPresent()) {
                IndicatorSnapshot value = indicator.orElseThrow();
                if (value.ma20() != null && value.ma60() != null
                        && value.ma20().compareTo(value.ma60()) > 0) {
                    reasons.add("MA20_ABOVE_MA60_UPTREND");
                }
                if (currentPrice != null && value.ma60() != null
                        && currentPrice.compareTo(value.ma60()) < 0) {
                    reasons.add("RISK_CURRENT_PRICE_BELOW_MA60");
                }
                if (value.ma5() != null && value.ma20() != null
                        && value.ma60() != null
                        && value.ma5().compareTo(value.ma20()) < 0
                        && value.ma20().compareTo(value.ma60()) < 0) {
                    adjustment -= 10;
                    reasons.add("MA5_MA20_MA60_BEARISH_ALIGNMENT");
                }
            } else if (result != null) {
                reasons.add("INDICATOR_DATA_INSUFFICIENT");
            }
            return new Assessment(excluded, adjustment,
                    reasons.stream().distinct().toList());
        }

        private Optional<IndicatorSnapshot> latest(String stockCode) {
            if (snapshotPort == null || baseDate == null) {
                return Optional.empty();
            }
            return snapshotPort.findByStockCodeAndTradeDateBetween(
                            stockCode, baseDate.minusYears(1), baseDate)
                    .stream()
                    .max(Comparator.comparing(
                            IndicatorSnapshot::tradeDate));
        }
    }

    record Assessment(
            boolean excluded,
            int scoreAdjustment,
            List<String> reasons
    ) {
    }
}
