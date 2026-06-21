package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.util.List;
import java.util.Optional;

interface TradingAccountJpaRepository extends JpaRepository<TradingAccountEntity, Long> {
    List<TradingAccountEntity> findAllByOrderByEnvironmentAscAliasAsc();
    Optional<TradingAccountEntity> findFirstByEnvironmentAndActiveTrueAndPrimaryAccountTrue(KisEnvironment environment);
    Optional<TradingAccountEntity> findFirstByActiveTrueAndPrimaryAccountTrue();
    @Modifying
    @Query("update TradingAccountEntity a set a.primaryAccount=false where a.environment=:environment")
    void clearPrimary(KisEnvironment environment);
    @Modifying
    @Query("update TradingAccountEntity a set a.primaryAccount=false")
    void clearAllPrimary();
}
