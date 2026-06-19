package seokhoon.trade.adapter.research.consensus;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.out.BrokerPort;
import seokhoon.trade.adapter.persistence.EarningsConsensusSnapshotEntity;
import seokhoon.trade.adapter.persistence.TargetPriceConsensusSnapshotEntity;

import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DisabledConsensusProviderAdapterTest {

    @Test
    void returnsNoExternalDataAndHasNoOrderDependency() {
        DisabledConsensusProviderAdapter adapter = new DisabledConsensusProviderAdapter();

        assertThat(adapter.fetchEarningsConsensus("005930", LocalDate.MIN, LocalDate.MAX)).isEmpty();
        assertThat(adapter.fetchTargetPriceConsensus("005930", LocalDate.MIN, LocalDate.MAX)).isEmpty();
        assertThat(Arrays.stream(DisabledConsensusProviderAdapter.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == BrokerPort.class)).isTrue();
    }

    @Test
    void persistenceModelDoesNotStorePaidReportContent() {
        assertThat(Arrays.stream(EarningsConsensusSnapshotEntity.class.getDeclaredFields())
                .map(field -> field.getName())).doesNotContain("reportContent", "reportOriginal", "reportUrl");
        assertThat(Arrays.stream(TargetPriceConsensusSnapshotEntity.class.getDeclaredFields())
                .map(field -> field.getName())).doesNotContain("reportContent", "reportOriginal", "reportUrl");
    }
}
