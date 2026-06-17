package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.EarningsPreview;

import java.time.LocalDate;

public interface GenerateEarningsPreviewUseCase {
    EarningsPreview generate(String stockCode, long earningsEventId, LocalDate previewDate);
}
