package seokhoon.trade.adapter.persistence;

import org.springframework.stereotype.Component;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.kis.KisEnvironment;
import java.util.*;

@Component
public class ExternalApiConfigurationPersistenceAdapter implements ExternalApiConfigurationPort {
    private final KisApiConfigurationJpaRepository kis;private final DartApiConfigurationJpaRepository dart;private final TradingAccountEncryptionPort encryption;
    public ExternalApiConfigurationPersistenceAdapter(KisApiConfigurationJpaRepository kis,DartApiConfigurationJpaRepository dart,TradingAccountEncryptionPort encryption){this.kis=kis;this.dart=dart;this.encryption=encryption;}
    public List<KisCredentials> findAllKis(){if(!encryption.configured())return List.of();return kis.findAll().stream().map(e->e.toDomain(encryption)).toList();}
    public Optional<KisCredentials> findKis(KisEnvironment env){if(!encryption.configured())return Optional.empty();return kis.findById(env).map(e->e.toDomain(encryption));}
    public KisCredentials saveKis(KisConfigCommand c){var e=kis.findById(c.environment()).orElseGet(KisApiConfigurationEntity::new);e.update(c.environment(),c.appKey(),c.appSecret(),c.baseUrl(),c.active(),encryption);return kis.saveAndFlush(e).toDomain(encryption);}
    public Optional<DartCredentials> findDart(){if(!encryption.configured())return Optional.empty();return dart.findById(1L).map(e->e.toDomain(encryption));}
    public DartCredentials saveDart(DartConfigCommand c){var e=dart.findById(1L).orElseGet(DartApiConfigurationEntity::new);e.update(c.apiKey(),c.baseUrl(),c.active(),encryption);return dart.saveAndFlush(e).toDomain(encryption);}
}
