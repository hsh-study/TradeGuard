package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ClosingBetBriefingResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BriefingControllerTest {
    @Test
    void sendsClosingBetBriefing() {
        LocalDate signalDate = LocalDate.of(2026, 6, 5);
        BriefingController controller = new BriefingController(date -> {
            assertThat(date).isEqualTo(signalDate);
            return new ClosingBetBriefingResult(
                    true,
                    2,
                    1,
                    "종가베팅 후보 2개, 리스크 후보 1개",
                    "body",
                    date
            );
        });

        BriefingController.ClosingBetBriefingResponse response = controller.sendClosingBet(signalDate);

        assertThat(response.sent()).isTrue();
        assertThat(response.candidateCount()).isEqualTo(2);
        assertThat(response.riskCandidateCount()).isEqualTo(1);
        assertThat(response.summary()).contains("종가베팅 후보 2개");
        assertThat(response.signalDate()).isEqualTo(signalDate);
    }
}
