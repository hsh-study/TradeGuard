package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.market.Sector;

import java.util.List;
import java.util.Optional;

public interface SectorPort {
    Sector save(Sector sector);
    List<Sector> findAll();
    Optional<Sector> findBySectorCode(String sectorCode);
}
