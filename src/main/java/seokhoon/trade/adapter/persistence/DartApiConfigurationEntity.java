package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase.DartCredentials;
import seokhoon.trade.application.port.out.TradingAccountEncryptionPort;
import java.time.Instant;

@Entity @Table(name="dart_api_configuration")
class DartApiConfigurationEntity {
    @Id Long id;
    @Column(name="encrypted_api_key",nullable=false,length=1024) String encryptedApiKey;
    @Column(name="base_url",nullable=false,length=500) String baseUrl;
    @Column(nullable=false) boolean active;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    protected DartApiConfigurationEntity(){}
    DartCredentials toDomain(TradingAccountEncryptionPort e){return new DartCredentials(e.decrypt(encryptedApiKey),baseUrl,active);}
    void update(String key,String url,boolean enabled,TradingAccountEncryptionPort e){id=1L;encryptedApiKey=e.encrypt(key);baseUrl=url;active=enabled;updatedAt=Instant.now();}
}
