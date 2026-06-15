package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.kis.*;

import java.time.*;

@Entity
@Table(name="kis_access_tokens")
class KisAccessTokenEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,unique=true) KisEnvironment environment;
    @Column(name="token_type") String tokenType;
    @Column(name="encrypted_access_token",length=4096)
    String encryptedAccessToken;
    @Column(name="issued_at") Instant issuedAt;
    @Column(name="expires_at") Instant expiresAt;
    @Column(name="daily_issued_date") LocalDate dailyIssuedDate;
    @Column(name="refresh_started_at") Instant refreshStartedAt;
    @Column(name="refresh_owner") String refreshOwner;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;

    protected KisAccessTokenEntity() {}

    void updateToken(StoredKisAccessToken value,Instant now) {
        tokenType=value.tokenType();
        encryptedAccessToken=value.encryptedAccessToken();
        issuedAt=value.issuedAt();
        expiresAt=value.expiresAt();
        dailyIssuedDate=value.dailyIssuedDate();
        updatedAt=now;
    }

    StoredKisAccessToken toDomain() {
        return new StoredKisAccessToken(environment,tokenType,
                encryptedAccessToken,issuedAt,expiresAt,dailyIssuedDate,
                refreshStartedAt,refreshOwner);
    }
}
