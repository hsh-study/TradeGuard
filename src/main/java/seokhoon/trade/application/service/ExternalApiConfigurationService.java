package seokhoon.trade.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase;
import seokhoon.trade.application.port.out.ExternalApiConfigurationPort;
import seokhoon.trade.application.port.out.TradingAccountEncryptionPort;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ExternalApiConfigurationService implements ExternalApiConfigurationUseCase {
    private final ExternalApiConfigurationPort configurations;
    private final TradingAccountEncryptionPort encryption;
    public ExternalApiConfigurationService(ExternalApiConfigurationPort configurations,
            TradingAccountEncryptionPort encryption) { this.configurations=configurations;this.encryption=encryption; }

    @Override @Transactional(readOnly=true) public List<KisConfigView> kisConfigs(){
        return configurations.findAllKis().stream().map(this::view).toList();
    }
    @Override @Transactional public KisConfigView saveKis(KisConfigCommand command){
        requireEncryption(); validateUrl(command.baseUrl());
        if(command.environment()==null||blank(command.appKey())||blank(command.appSecret()))
            throw new IllegalArgumentException("KIS environment, appKey and appSecret are required");
        return view(configurations.saveKis(command));
    }
    @Override @Transactional(readOnly=true) public Optional<KisCredentials> kisCredentials(KisEnvironment environment){
        return configurations.findKis(environment).filter(KisCredentials::active);
    }
    @Override @Transactional(readOnly=true) public Optional<DartCredentials> dartCredentials(){return configurations.findDart().filter(DartCredentials::active);}
    @Override @Transactional public DartConfigView saveDart(DartConfigCommand command){
        requireEncryption();validateUrl(command.baseUrl());if(blank(command.apiKey()))throw new IllegalArgumentException("DART apiKey is required");
        return view(configurations.saveDart(command));
    }
    @Override @Transactional(readOnly=true) public Optional<DartConfigView> dartConfig(){return configurations.findDart().map(this::view);}
    private KisConfigView view(KisCredentials c){return new KisConfigView(c.environment(),!blank(c.appKey()),!blank(c.appSecret()),mask(c.appKey()),c.baseUrl(),c.active(),Instant.now());}
    private DartConfigView view(DartCredentials c){return new DartConfigView(!blank(c.apiKey()),mask(c.apiKey()),c.baseUrl(),c.active(),Instant.now());}
    private void requireEncryption(){if(!encryption.configured())throw new IllegalStateException("KIS_TOKEN_ENCRYPTION_KEY is required");}
    private static void validateUrl(String value){if(blank(value)||!(value.startsWith("https://")||value.startsWith("http://localhost")||value.startsWith("http://127.0.0.1")))throw new IllegalArgumentException("baseUrl must use HTTPS");}
    private static boolean blank(String v){return v==null||v.isBlank();}
    private static String mask(String v){if(blank(v))return "";return "*".repeat(Math.max(4,v.length()-4))+v.substring(Math.max(0,v.length()-4));}
}
