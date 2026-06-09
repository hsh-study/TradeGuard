package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.audit.AuditActor;

import java.util.UUID;

public interface CorrelationIdProvider {
    String currentCorrelationId();

    String newCorrelationId();

    default AuditActor currentActor() {
        return AuditActor.SYSTEM;
    }

    static CorrelationIdProvider generated() {
        return new CorrelationIdProvider() {
            @Override
            public String currentCorrelationId() {
                return newCorrelationId();
            }

            @Override
            public String newCorrelationId() {
                return UUID.randomUUID().toString();
            }
        };
    }
}
