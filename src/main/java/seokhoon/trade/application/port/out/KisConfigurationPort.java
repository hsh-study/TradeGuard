package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.kis.KisEnvironment;

public interface KisConfigurationPort {
    KisEnvironment readOnlyEnvironment();
    boolean credentialsConfigured();
    int tokenRefreshBeforeSeconds();
}
