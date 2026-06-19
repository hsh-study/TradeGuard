package seokhoon.trade.application.port.in;

import java.time.LocalDate;

public interface GetOperationalDashboardUseCase {
    OperationalDashboardSummary getDashboard(LocalDate baseDate);
    OperationalDashboardSummary getTodayDashboard();
}
