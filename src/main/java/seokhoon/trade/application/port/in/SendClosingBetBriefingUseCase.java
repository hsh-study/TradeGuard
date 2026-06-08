package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface SendClosingBetBriefingUseCase {
    ClosingBetBriefingResult send(LocalDate signalDate);
}
