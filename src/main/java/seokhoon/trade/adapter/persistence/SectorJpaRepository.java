package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SectorJpaRepository extends JpaRepository<SectorEntity, Long> {
    Optional<SectorEntity> findBySectorCode(String sectorCode);
}
