package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.EarningsEvent;
import seokhoon.trade.domain.research.EarningsEventStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EarningsEventPort {
    EarningsEvent save(EarningsEvent value);
    Optional<EarningsEvent> findById(long id);
    Optional<EarningsEvent> findEventByStockCodeAndQuarter(String stockCode, int fiscalYear, int fiscalQuarter);
    List<EarningsEvent> find(String stockCode, LocalDate from, LocalDate to);
    List<EarningsEvent> findByStatusAndExpectedAnnouncementDateBetween(
            EarningsEventStatus status,
            LocalDate from,
            LocalDate to
    );
}
