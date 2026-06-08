package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.ClosingBetBriefingResult;
import seokhoon.trade.application.port.in.SendClosingBetBriefingUseCase;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/briefings")
public class BriefingController {
    private final SendClosingBetBriefingUseCase sendClosingBetBriefingUseCase;

    public BriefingController(SendClosingBetBriefingUseCase sendClosingBetBriefingUseCase) {
        this.sendClosingBetBriefingUseCase = sendClosingBetBriefingUseCase;
    }

    @PostMapping("/closing-bet")
    ClosingBetBriefingResponse sendClosingBet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate signalDate
    ) {
        return ClosingBetBriefingResponse.from(sendClosingBetBriefingUseCase.send(signalDate));
    }

    public record ClosingBetBriefingResponse(
            boolean sent,
            int candidateCount,
            int riskCandidateCount,
            String summary,
            LocalDate signalDate
    ) {
        static ClosingBetBriefingResponse from(ClosingBetBriefingResult result) {
            return new ClosingBetBriefingResponse(
                    result.sent(),
                    result.candidateCount(),
                    result.riskCandidateCount(),
                    result.summary(),
                    result.signalDate()
            );
        }
    }
}
