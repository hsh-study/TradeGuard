package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockJpaRepository extends JpaRepository<StockEntity, String> {
}
