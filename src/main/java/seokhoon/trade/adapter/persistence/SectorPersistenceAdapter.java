package seokhoon.trade.adapter.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.SectorPort;
import seokhoon.trade.domain.market.Sector;

import java.util.List;
import java.util.Optional;

@Component
public class SectorPersistenceAdapter implements SectorPort {
    private final SectorJpaRepository repository;

    public SectorPersistenceAdapter(SectorJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Sector save(Sector sector) {
        SectorEntity entity = repository.findBySectorCode(sector.sectorCode())
                .orElseGet(() -> SectorEntity.from(sector));
        entity.update(sector);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sector> findAll() {
        return repository.findAll(Sort.by(Sort.Order.asc("sectorCode")))
                .stream().map(SectorEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sector> findBySectorCode(String sectorCode) {
        return repository.findBySectorCode(sectorCode).map(SectorEntity::toDomain);
    }
}
