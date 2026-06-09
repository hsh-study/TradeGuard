package seokhoon.trade.adapter.correlation;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.out.CorrelationIdProvider;
import seokhoon.trade.domain.audit.AuditActor;

import java.util.UUID;

@Component
public class MdcCorrelationIdProvider implements CorrelationIdProvider {
    static final String REQUEST_ID_MDC_KEY = "requestId";
    static final String SCHEDULER_CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public String currentCorrelationId() {
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        String schedulerCorrelationId = MDC.get(SCHEDULER_CORRELATION_ID_MDC_KEY);
        return schedulerCorrelationId == null || schedulerCorrelationId.isBlank()
                ? newCorrelationId()
                : schedulerCorrelationId;
    }

    @Override
    public String newCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public AuditActor currentActor() {
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        if (requestId != null && !requestId.isBlank()) {
            return AuditActor.API;
        }
        String schedulerCorrelationId = MDC.get(SCHEDULER_CORRELATION_ID_MDC_KEY);
        return schedulerCorrelationId == null || schedulerCorrelationId.isBlank()
                ? AuditActor.SYSTEM
                : AuditActor.SCHEDULER;
    }
}
