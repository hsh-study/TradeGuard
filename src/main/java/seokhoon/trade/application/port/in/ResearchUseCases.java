package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ResearchUseCases {
    private ResearchUseCases() {
    }

    public record CreateThesisCommand(
            String stockCode,
            String title,
            String coreAssumption,
            String invalidationCondition,
            BigDecimal targetPrice,
            String stopLossCondition,
            int confidence,
            ThesisStatus status
    ) {
    }

    public record UpdateThesisCommand(
            String title,
            String coreAssumption,
            String invalidationCondition,
            BigDecimal targetPrice,
            String stopLossCondition,
            Integer confidence,
            ThesisStatus status
    ) {
    }

    public interface ThesisUseCase {
        InvestmentThesis create(CreateThesisCommand command);
        List<InvestmentThesis> find(String stockCode);
        InvestmentThesis update(long id, UpdateThesisCommand command);
        InvestmentThesis close(long id);
    }

    public record CreateCatalystCommand(
            String stockCode,
            String title,
            CatalystType catalystType,
            LocalDate expectedDate,
            CatalystImportance importance,
            CatalystStatus status,
            String sourceUrl,
            String memo
    ) {
    }

    public record UpdateCatalystCommand(
            String stockCode,
            String title,
            CatalystType catalystType,
            LocalDate expectedDate,
            CatalystImportance importance,
            CatalystStatus status,
            String sourceUrl,
            String memo
    ) {
    }

    public interface CatalystUseCase {
        InvestmentCatalyst create(CreateCatalystCommand command);
        List<InvestmentCatalyst> find(String stockCode, LocalDate from, LocalDate to);
        InvestmentCatalyst update(long id, UpdateCatalystCommand command);
    }

    public interface MorningNoteUseCase {
        MorningNote generate(LocalDate tradeDate);
        MorningNote load(LocalDate tradeDate);
    }
}
