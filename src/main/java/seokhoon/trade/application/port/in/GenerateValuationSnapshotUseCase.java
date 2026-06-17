package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.ValuationGenerationResult;

import java.time.LocalDate;
import java.util.List;

public interface GenerateValuationSnapshotUseCase {
    ValuationGenerationResult generate(String stockCode, LocalDate baseDate);
    List<ValuationGenerationResult> generateBatch(List<String> stockCodes, LocalDate baseDate);
    List<ValuationGenerationResult> generateWatchlist(LocalDate baseDate);
}
