package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.position.LivePositionExitRule;

import java.util.Optional;

public interface LivePositionExitRulePort {
    LivePositionExitRule save(LivePositionExitRule rule);
    Optional<LivePositionExitRule> findByPositionId(long positionId);
}
