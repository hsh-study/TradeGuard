package seokhoon.trade.adapter.correlation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import seokhoon.trade.domain.audit.AuditActor;

import static org.assertj.core.api.Assertions.assertThat;

class MdcCorrelationIdProviderTest {
    private final MdcCorrelationIdProvider provider = new MdcCorrelationIdProvider();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void prefersHttpRequestId() {
        MDC.put(MdcCorrelationIdProvider.REQUEST_ID_MDC_KEY, "request-123");
        MDC.put(
                MdcCorrelationIdProvider.SCHEDULER_CORRELATION_ID_MDC_KEY,
                "scheduler-123"
        );

        assertThat(provider.currentCorrelationId()).isEqualTo("request-123");
        assertThat(provider.currentActor()).isEqualTo(AuditActor.API);
    }

    @Test
    void usesSchedulerCorrelationIdOutsideHttpRequest() {
        MDC.put(
                MdcCorrelationIdProvider.SCHEDULER_CORRELATION_ID_MDC_KEY,
                "scheduler-123"
        );

        assertThat(provider.currentCorrelationId()).isEqualTo("scheduler-123");
        assertThat(provider.currentActor()).isEqualTo(AuditActor.SCHEDULER);
    }

    @Test
    void generatesCorrelationIdWhenMdcIsEmpty() {
        assertThat(provider.currentCorrelationId()).isNotBlank();
        assertThat(provider.currentActor()).isEqualTo(AuditActor.SYSTEM);
    }
}
