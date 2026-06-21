package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.IntradayMarketSnapshot;
import seokhoon.trade.application.port.out.MarketSnapshotPort;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RealtimeStockChartServiceTest {
    @Test void sharesPollingAndStopsAfterLastSubscriberCloses() throws Exception {
        MarketSnapshotPort snapshots=mock(MarketSnapshotPort.class);
        when(snapshots.getSnapshot("005930")).thenReturn(Optional.of(new IntradayMarketSnapshot(
                "005930",new BigDecimal("72000"),new BigDecimal("1.2"),new BigDecimal("72500"),
                new BigDecimal("71000"),1234,new BigDecimal("88000000"),new BigDecimal("71800"),Instant.now())));
        Clock clock=Clock.fixed(Instant.parse("2026-06-22T01:00:00Z"),ZoneId.of("Asia/Seoul"));
        RealtimeStockChartService service=new RealtimeStockChartService(snapshots,2000,1,clock);
        CountDownLatch latch=new CountDownLatch(2);
        var first=service.subscribe("005930",point->latch.countDown());
        var second=service.subscribe("005930",point->latch.countDown());
        assertThat(latch.await(1,TimeUnit.SECONDS)).isTrue();
        verify(snapshots,times(1)).getSnapshot("005930");
        first.close();second.close();service.close();
    }

    @Test void rejectsInvalidStockCode(){
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                ()->RealtimeStockChartService.requireCode("../secret")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void doesNotCallProviderOutsideMarketHours() throws Exception {
        MarketSnapshotPort snapshots=mock(MarketSnapshotPort.class);
        Clock sunday=Clock.fixed(Instant.parse("2026-06-21T01:00:00Z"),ZoneId.of("Asia/Seoul"));
        RealtimeStockChartService service=new RealtimeStockChartService(snapshots,2000,1,sunday);
        var subscription=service.subscribe("005930",point->{});Thread.sleep(100);
        verifyNoInteractions(snapshots);subscription.close();service.close();
    }
}
