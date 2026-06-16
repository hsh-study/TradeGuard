package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.InvestmentCatalystPort;
import seokhoon.trade.application.port.out.InvestmentThesisPort;
import seokhoon.trade.application.port.out.MorningNotePort;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ResearchPersistenceIntegrationTest {
    @Autowired
    private InvestmentThesisPort theses;
    @Autowired
    private InvestmentCatalystPort catalysts;
    @Autowired
    private MorningNotePort notes;

    @Test
    void storesAndQueriesResearchWorkflowModels() {
        Instant now = Instant.parse("2026-06-15T00:00:00Z");
        InvestmentThesis thesis = theses.save(new InvestmentThesis(
                null, "005930", "memory recovery", "margin improves",
                "margin declines", new BigDecimal("90000"), "below MA60", 70,
                ThesisStatus.ACTIVE, now, now));
        InvestmentCatalyst catalyst = catalysts.save(new InvestmentCatalyst(
                null, "005930", "2Q earnings", CatalystType.EARNINGS,
                LocalDate.of(2026, 7, 31), CatalystImportance.HIGH,
                CatalystStatus.UPCOMING, null, "check margin", now, now));
        MorningNote note = notes.save(new MorningNote(
                null, LocalDate.of(2026, 6, 15), "market", "sector",
                "portfolio", "watchlist", "actions", now));

        assertThat(theses.findThesisById(thesis.id())).isPresent();
        assertThat(theses.find("005930", ThesisStatus.ACTIVE)).hasSize(1);
        assertThat(catalysts.findCatalystById(catalyst.id())).isPresent();
        assertThat(catalysts.find("005930", LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1), CatalystStatus.UPCOMING)).hasSize(1);
        assertThat(notes.findByTradeDate(note.tradeDate())).isPresent();
    }
}
