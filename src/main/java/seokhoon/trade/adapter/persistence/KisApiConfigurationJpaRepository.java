package seokhoon.trade.adapter.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import seokhoon.trade.domain.kis.KisEnvironment;
interface KisApiConfigurationJpaRepository extends JpaRepository<KisApiConfigurationEntity,KisEnvironment>{}
