package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase.KisCredentials;
import seokhoon.trade.application.port.out.TradingAccountEncryptionPort;
import seokhoon.trade.domain.kis.KisEnvironment;
import java.time.Instant;

@Entity @Table(name="kis_api_configurations")
class KisApiConfigurationEntity {
    @Id @Enumerated(EnumType.STRING) @Column(length=10) KisEnvironment environment;
    @Column(name="encrypted_app_key",nullable=false,length=1024) String encryptedAppKey;
    @Column(name="encrypted_app_secret",nullable=false,length=2048) String encryptedAppSecret;
    @Column(name="base_url",nullable=false,length=500) String baseUrl;
    @Column(nullable=false) boolean active;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    protected KisApiConfigurationEntity(){}
    KisCredentials toDomain(TradingAccountEncryptionPort e){return new KisCredentials(environment,e.decrypt(encryptedAppKey),e.decrypt(encryptedAppSecret),baseUrl,active);}
    void update(KisEnvironment env,String key,String secret,String url,boolean enabled,TradingAccountEncryptionPort e){environment=env;encryptedAppKey=e.encrypt(key);encryptedAppSecret=e.encrypt(secret);baseUrl=url;active=enabled;updatedAt=Instant.now();}
}
