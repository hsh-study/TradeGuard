package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateQuarterlyFinancialCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.CreateValuationSnapshotCommand;
import seokhoon.trade.application.port.in.ResearchUseCases.EarningsDataUseCase;
import seokhoon.trade.application.port.out.OperationalMetricsPort;
import seokhoon.trade.application.port.out.QuarterlyFinancialPort;
import seokhoon.trade.application.port.out.SharesOutstandingSnapshotPort;
import seokhoon.trade.application.port.out.ValuationSnapshotPort;
import seokhoon.trade.domain.research.QuarterlyFinancial;
import seokhoon.trade.domain.research.SharesOutstandingSnapshot;
import seokhoon.trade.domain.research.ValuationSnapshotSource;
import seokhoon.trade.domain.research.ValuationSnapshot;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class EarningsDataService implements EarningsDataUseCase {
    private final QuarterlyFinancialPort financialPort;
    private final ValuationSnapshotPort valuationPort;
    private final SharesOutstandingSnapshotPort sharesOutstandingPort;
    private final OperationalMetricsPort metrics;
    private final Clock clock;

    @Autowired
    public EarningsDataService(
            QuarterlyFinancialPort financialPort,
            ValuationSnapshotPort valuationPort,
            SharesOutstandingSnapshotPort sharesOutstandingPort,
            OperationalMetricsPort metrics
    ) {
        this(financialPort, valuationPort, sharesOutstandingPort, metrics, Clock.systemUTC());
    }

    EarningsDataService(
            QuarterlyFinancialPort financialPort,
            ValuationSnapshotPort valuationPort,
            SharesOutstandingSnapshotPort sharesOutstandingPort,
            OperationalMetricsPort metrics,
            Clock clock
    ) {
        this.financialPort = financialPort;
        this.valuationPort = valuationPort;
        this.sharesOutstandingPort = sharesOutstandingPort;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<QuarterlyFinancial> saveQuarterly(List<CreateQuarterlyFinancialCommand> commands) {
        try {
            Instant now = clock.instant();
            List<QuarterlyFinancial> saved = financialPort.saveAll(commands.stream()
                    .map(command -> new QuarterlyFinancial(
                            null,
                            command.stockCode(),
                            command.fiscalYear(),
                            command.fiscalQuarter(),
                            command.revenue(),
                            command.operatingIncome(),
                            command.netIncome(),
                            command.totalAssets(),
                            command.totalLiabilities(),
                            command.totalEquity(),
                            command.operatingCashFlow(),
                            command.freeCashFlow(),
                            now,
                            now
                    ))
                    .toList());
            metrics.recordResearchFinancialImport("saved");
            return saved;
        } catch (RuntimeException exception) {
            metrics.recordResearchFinancialImport("failure");
            throw exception;
        }
    }

    @Override
    @Transactional
    public ValuationSnapshot saveValuation(CreateValuationSnapshotCommand command) {
        try {
            Instant now = clock.instant();
            ValuationSnapshot saved = valuationPort.save(new ValuationSnapshot(
                    null,
                    command.stockCode(),
                    command.tradeDate(),
                    command.marketCap(),
                    command.per(),
                    command.pbr(),
                    command.psr(),
                    command.eps(),
                    command.bps(),
                    command.salesPerShare(),
                    command.source() == null ? ValuationSnapshotSource.MANUAL : command.source(),
                    now,
                    now
            ));
            metrics.recordResearchValuationImport("saved");
            return saved;
        } catch (RuntimeException exception) {
            metrics.recordResearchValuationImport("failure");
            throw exception;
        }
    }

    @Override
    @Transactional
    public SharesOutstandingSnapshot saveSharesOutstanding(
            seokhoon.trade.application.port.in.ResearchUseCases.SaveSharesOutstandingCommand command
    ) {
        try {
            Instant now = clock.instant();
            SharesOutstandingSnapshot saved = sharesOutstandingPort.save(new SharesOutstandingSnapshot(
                    null,
                    command.stockCode(),
                    command.baseDate(),
                    command.sharesOutstanding(),
                    command.source(),
                    now,
                    now
            ));
            metrics.recordResearchSharesOutstanding("saved");
            return saved;
        } catch (RuntimeException exception) {
            metrics.recordResearchSharesOutstanding("failure");
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharesOutstandingSnapshot> findSharesOutstanding(String stockCode) {
        return sharesOutstandingPort.findSharesByStockCode(stockCode);
    }
}
