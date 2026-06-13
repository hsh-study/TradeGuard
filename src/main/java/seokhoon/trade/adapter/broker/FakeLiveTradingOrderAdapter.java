package seokhoon.trade.adapter.broker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.order.*;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "tradeguard.live-trading.kis-trading-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class FakeLiveTradingOrderAdapter implements LiveTradingOrderPort {
    @Override public LiveOrderSubmission submitBuyLimitOrder(LiveOrderRequest order){return accepted();}
    @Override public LiveOrderSubmission submitSellLimitOrder(LiveOrderRequest order){return accepted();}
    @Override public LiveOrderSubmission inquireOrder(LiveOrderRequest order){return LiveOrderSubmission.accepted(order.kisOrderNo(),order.kisOriginalOrderNo());}
    @Override public List<LiveTradeFill> inquireFilledOrders(List<LiveOrderRequest> orders){return List.of();}
    private static LiveOrderSubmission accepted(){return LiveOrderSubmission.accepted("FAKE-"+UUID.randomUUID(),null);}
}
