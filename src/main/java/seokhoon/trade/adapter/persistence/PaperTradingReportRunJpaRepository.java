package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface PaperTradingReportRunJpaRepository extends JpaRepository<PaperTradingReportRunEntity, Long> {
    Optional<PaperTradingReportRunEntity> findFirstByTradeDateOrderByCreatedAtDescIdDesc(LocalDate tradeDate);
}
