package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.*;
import java.util.Objects;

public record EarningsConsensusSnapshot(Long id,String stockCode,int fiscalYear,int fiscalQuarter,
        LocalDate consensusDate,BigDecimal expectedRevenue,BigDecimal expectedOperatingIncome,
        BigDecimal expectedNetIncome,BigDecimal expectedOperatingMargin,Integer analystCount,
        ConsensusSource source,String providerName,ConsensusStatus status,Instant createdAt,Instant updatedAt) {
    public EarningsConsensusSnapshot {Objects.requireNonNull(stockCode);Objects.requireNonNull(consensusDate);
        Objects.requireNonNull(source);Objects.requireNonNull(status);Objects.requireNonNull(createdAt);Objects.requireNonNull(updatedAt);
        if(fiscalQuarter<1||fiscalQuarter>4)throw new IllegalArgumentException("fiscalQuarter must be 1..4");}
}
