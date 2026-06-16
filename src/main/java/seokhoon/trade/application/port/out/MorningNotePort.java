package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.MorningNote;

import java.time.LocalDate;
import java.util.Optional;

public interface MorningNotePort {
    MorningNote save(MorningNote note);
    Optional<MorningNote> findByTradeDate(LocalDate tradeDate);
}
