package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.*;
import java.util.Objects;

public record TargetPriceConsensusSnapshot(Long id,String stockCode,LocalDate consensusDate,
        BigDecimal targetPrice,BigDecimal currentPrice,BigDecimal upsideRate,Integer analystCount,
        ConsensusSource source,String providerName,ConsensusStatus status,Instant createdAt,Instant updatedAt) {
    public TargetPriceConsensusSnapshot {Objects.requireNonNull(stockCode);Objects.requireNonNull(consensusDate);
        Objects.requireNonNull(targetPrice);Objects.requireNonNull(source);Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);Objects.requireNonNull(updatedAt);if(targetPrice.signum()<0)throw new IllegalArgumentException("targetPrice must not be negative");}
}
