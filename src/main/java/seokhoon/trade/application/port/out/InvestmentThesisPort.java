package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.InvestmentThesis;
import seokhoon.trade.domain.research.ThesisStatus;

import java.util.List;
import java.util.Optional;

public interface InvestmentThesisPort {
    InvestmentThesis save(InvestmentThesis thesis);
    Optional<InvestmentThesis> findThesisById(long id);
    List<InvestmentThesis> find(String stockCode, ThesisStatus status);
}
