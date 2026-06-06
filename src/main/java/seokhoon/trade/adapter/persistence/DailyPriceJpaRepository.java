package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyPriceJpaRepository extends JpaRepository<DailyPriceEntity, DailyPriceId> {
}
