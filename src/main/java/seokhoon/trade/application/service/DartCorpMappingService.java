package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.ResearchUseCases.DartCorpMappingUseCase;
import seokhoon.trade.application.port.in.ResearchUseCases.SaveDartCorpMappingCommand;
import seokhoon.trade.application.port.out.DartCorpMappingPort;
import seokhoon.trade.domain.research.DartCorpMapping;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DartCorpMappingService implements DartCorpMappingUseCase {
    private final DartCorpMappingPort port;
    private final Clock clock;

    @Autowired
    public DartCorpMappingService(DartCorpMappingPort port) {
        this(port, Clock.systemUTC());
    }

    DartCorpMappingService(DartCorpMappingPort port, Clock clock) {
        this.port = port;
        this.clock = clock;
    }

    @Override
    public DartCorpMapping save(SaveDartCorpMappingCommand command) {
        Instant now = clock.instant();
        return port.save(new DartCorpMapping(null, command.stockCode(), command.corpCode(),
                command.corpName(), command.market(), now, now));
    }

    @Override
    public Optional<DartCorpMapping> findByStockCode(String stockCode) {
        return port.findByStockCode(stockCode);
    }

    @Override
    public List<DartCorpMapping> findAll() {
        return port.findAll();
    }
}
