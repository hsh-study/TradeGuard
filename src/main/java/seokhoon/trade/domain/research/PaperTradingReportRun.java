package seokhoon.trade.domain.research;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaperTradingReportRun(Long id, LocalDate tradeDate, PaperTradingReportStatus status,
                                    int totalCandidates, BigDecimal averageReturnRate, int winCount,
                                    int lossCount, int flatCount, String failureReason,
                                    Instant createdAt, Instant completedAt) { }
