package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.CatalystStatus;
import seokhoon.trade.domain.research.InvestmentCatalyst;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvestmentCatalystPort {
    InvestmentCatalyst save(InvestmentCatalyst catalyst);
    Optional<InvestmentCatalyst> findCatalystById(long id);
    List<InvestmentCatalyst> find(String stockCode, LocalDate from, LocalDate to, CatalystStatus status);
}
