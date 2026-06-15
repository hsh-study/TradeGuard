package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.time.Instant;
import java.util.Optional;

interface KisAccessTokenJpaRepository
        extends JpaRepository<KisAccessTokenEntity,Long> {
    Optional<KisAccessTokenEntity> findByEnvironment(
            KisEnvironment environment);

    @Modifying(clearAutomatically=true,flushAutomatically=true)
    @Query("""
            update KisAccessTokenEntity token
               set token.refreshStartedAt = :now,
                   token.refreshOwner = :owner,
                   token.updatedAt = :now
             where token.environment = :environment
               and (token.refreshStartedAt is null
                    or token.refreshStartedAt < :staleBefore)
            """)
    int tryAcquireRefreshLock(
            @Param("environment") KisEnvironment environment,
            @Param("owner") String owner,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore);

    @Modifying(clearAutomatically=true,flushAutomatically=true)
    @Query("""
            update KisAccessTokenEntity token
               set token.refreshStartedAt = null,
                   token.refreshOwner = null,
                   token.updatedAt = :now
             where token.environment = :environment
               and token.refreshOwner = :owner
            """)
    int releaseRefreshLock(
            @Param("environment") KisEnvironment environment,
            @Param("owner") String owner,
            @Param("now") Instant now);

    @Modifying(clearAutomatically=true,flushAutomatically=true)
    @Query("""
            update KisAccessTokenEntity token
               set token.tokenType = null,
                   token.encryptedAccessToken = null,
                   token.issuedAt = null,
                   token.expiresAt = null,
                   token.dailyIssuedDate = null,
                   token.refreshStartedAt = null,
                   token.refreshOwner = null,
                   token.updatedAt = :now
             where token.environment = :environment
            """)
    int clear(@Param("environment") KisEnvironment environment,
            @Param("now") Instant now);
}
