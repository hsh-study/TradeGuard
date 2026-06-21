package seokhoon.trade.application.port.in;

import seokhoon.trade.domain.operations.BootReadinessReport;

import java.util.Optional;

public interface GetBootReadinessReportUseCase {
    Optional<BootReadinessReport> getLatestReport();
}
