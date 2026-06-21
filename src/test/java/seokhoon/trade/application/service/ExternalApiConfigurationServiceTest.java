package seokhoon.trade.application.service;

import org.junit.jupiter.api.Test;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase.*;
import seokhoon.trade.application.port.out.*;
import seokhoon.trade.domain.kis.KisEnvironment;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ExternalApiConfigurationServiceTest {
    @Test void storesSeparateDemoRealKisAndDartConfigurations() {
        MemoryPort port=new MemoryPort();
        ExternalApiConfigurationService service=new ExternalApiConfigurationService(port,encryption());
        service.saveKis(new KisConfigCommand(KisEnvironment.DEMO,"demo-key","demo-secret","https://demo.example",true));
        service.saveKis(new KisConfigCommand(KisEnvironment.REAL,"real-key","real-secret","https://real.example",true));
        service.saveDart(new DartConfigCommand("dart-key","https://dart.example",true));
        assertThat(service.kisCredentials(KisEnvironment.DEMO)).get().extracting(KisCredentials::appKey).isEqualTo("demo-key");
        assertThat(service.kisCredentials(KisEnvironment.REAL)).get().extracting(KisCredentials::baseUrl).isEqualTo("https://real.example");
        assertThat(service.dartCredentials()).get().extracting(DartCredentials::apiKey).isEqualTo("dart-key");
        assertThat(service.kisConfigs()).extracting(KisConfigView::maskedAppKey).doesNotContain("demo-key","real-key");
    }
    private static TradingAccountEncryptionPort encryption(){return new TradingAccountEncryptionPort(){public boolean configured(){return true;}public String encrypt(String v){return "enc:"+v;}public String decrypt(String v){return v.substring(4);}};}
    private static final class MemoryPort implements ExternalApiConfigurationPort {
        Map<KisEnvironment,KisCredentials> kis=new EnumMap<>(KisEnvironment.class);DartCredentials dart;
        public List<KisCredentials> findAllKis(){return List.copyOf(kis.values());}
        public Optional<KisCredentials> findKis(KisEnvironment e){return Optional.ofNullable(kis.get(e));}
        public KisCredentials saveKis(KisConfigCommand c){var v=new KisCredentials(c.environment(),c.appKey(),c.appSecret(),c.baseUrl(),c.active());kis.put(c.environment(),v);return v;}
        public Optional<DartCredentials> findDart(){return Optional.ofNullable(dart);}
        public DartCredentials saveDart(DartConfigCommand c){return dart=new DartCredentials(c.apiKey(),c.baseUrl(),c.active());}
    }
}
