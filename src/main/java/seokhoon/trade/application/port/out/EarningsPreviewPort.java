package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.EarningsPreview;
import seokhoon.trade.domain.research.EarningsPreviewStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarningsPreviewPort {
    EarningsPreview save(EarningsPreview value);
    Optional<EarningsPreview> findPreviewById(long id);
    Optional<EarningsPreview> findLatestByEarningsEventId(long earningsEventId);
    List<EarningsPreview> findPreviewsByStockCode(String stockCode);
    List<EarningsPreview> findByStatusAndPreviewDateBetween(EarningsPreviewStatus status, LocalDate from, LocalDate to);
}
