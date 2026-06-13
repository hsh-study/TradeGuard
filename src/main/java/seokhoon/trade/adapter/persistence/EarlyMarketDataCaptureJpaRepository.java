package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seokhoon.trade.domain.market.EarlyMarketCaptureType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarlyMarketDataCaptureJpaRepository
        extends JpaRepository<EarlyMarketDataCaptureEntity, Long> {
    Optional<EarlyMarketDataCaptureEntity> findByTradeDateAndCaptureType(
            LocalDate tradeDate,
            EarlyMarketCaptureType captureType
    );

    List<EarlyMarketDataCaptureEntity>
    findByTradeDateOrderByCapturedAtAscCaptureTypeAsc(LocalDate tradeDate);
}
