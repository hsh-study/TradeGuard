package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.kis.KisEnvironment;
import seokhoon.trade.domain.kis.KisTokenCacheMode;

public interface KisConfigurationPort {
    KisEnvironment readOnlyEnvironment();
    boolean credentialsConfigured();
    int tokenRefreshBeforeSeconds();
    KisTokenCacheMode tokenCacheMode();
    boolean tokenEncryptionConfigured();
}
