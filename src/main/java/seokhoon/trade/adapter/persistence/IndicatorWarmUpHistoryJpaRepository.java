package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IndicatorWarmUpHistoryJpaRepository
        extends JpaRepository<IndicatorWarmUpHistoryEntity, Long> {
    List<IndicatorWarmUpHistoryEntity>
    findByStockCodeOrderByCreatedAtDescIdDesc(String stockCode);
}
