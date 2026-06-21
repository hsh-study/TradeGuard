package seokhoon.trade.application.port.out;

import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase.*;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.util.List;
import java.util.Optional;

public interface ExternalApiConfigurationPort {
    List<KisCredentials> findAllKis();
    Optional<KisCredentials> findKis(KisEnvironment environment);
    KisCredentials saveKis(KisConfigCommand command);
    Optional<DartCredentials> findDart();
    DartCredentials saveDart(DartConfigCommand command);
}
