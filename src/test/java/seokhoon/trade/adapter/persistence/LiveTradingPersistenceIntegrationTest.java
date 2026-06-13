package seokhoon.trade.adapter.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.order.*;
import seokhoon.trade.domain.position.*;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LiveTradingPersistenceIntegrationTest {
    @Autowired LiveOrderRequestPort orders;
    @Autowired LivePositionPort positions;
    @Autowired LiveTradingRuntimeStatePort runtime;

    @Test
    void storesLiveOrderPositionAndKillSwitch(){
        Instant now=Instant.parse("2026-06-12T01:00:00Z");
        LiveOrderRequest order=orders.save(new LiveOrderRequest(null,9L,"005930",
                OrderSide.BUY,1,BigDecimal.valueOf(70000),OrderType.LIMIT,
                LiveOrderStatus.CREATED,null,null,null,now,null,now));
        LivePosition position=positions.savePosition(new LivePosition(null,"005930",1,
                BigDecimal.valueOf(70000),BigDecimal.valueOf(70000),
                BigDecimal.TEN,LivePositionStatus.OPEN,now,null));
        runtime.save(new LiveTradingRuntimeState(true,"test",now));

        assertThat(orders.findOrderById(order.id())).isPresent();
        assertThat(orders.existsBySignalIdAndSide(9L,OrderSide.BUY)).isTrue();
        assertThat(positions.findPositionById(position.id())).isPresent();
        assertThat(runtime.get().killSwitchEnabled()).isTrue();
    }
}
