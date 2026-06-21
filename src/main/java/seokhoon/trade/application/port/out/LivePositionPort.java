package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.position.LivePosition;

import java.util.List;
import java.util.Optional;
import seokhoon.trade.domain.kis.KisEnvironment;

public interface LivePositionPort {
    LivePosition savePosition(LivePosition position);
    List<LivePosition> findOpenPositions();
    Optional<LivePosition> findPositionById(long id);
    Optional<LivePosition> findByStockCode(String stockCode);
    default Optional<LivePosition> findByStockCodeAndEnvironment(
            String stockCode, KisEnvironment environment) {
        return findByStockCode(stockCode)
                .filter(position -> position.environment() == null
                        || position.environment() == environment);
    }
    LivePosition updatePosition(LivePosition position);
}
