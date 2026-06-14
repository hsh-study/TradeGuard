package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.kis.KisAccessToken;
import seokhoon.trade.domain.kis.KisEnvironment;

public interface KisTokenClient {
    KisAccessToken issueToken(
            KisEnvironment environment,
            String appKey,
            String appSecret
    );
}
