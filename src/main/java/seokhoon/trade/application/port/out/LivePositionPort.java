package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.position.LivePosition;

import java.util.List;
import java.util.Optional;

public interface LivePositionPort {
    LivePosition savePosition(LivePosition position);
    List<LivePosition> findOpenPositions();
    Optional<LivePosition> findPositionById(long id);
    Optional<LivePosition> findByStockCode(String stockCode);
    LivePosition updatePosition(LivePosition position);
}
