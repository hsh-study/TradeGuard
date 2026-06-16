package seokhoon.trade.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

interface InvestmentThesisJpaRepository extends JpaRepository<InvestmentThesisEntity, Long>,
        JpaSpecificationExecutor<InvestmentThesisEntity> {
}

interface InvestmentCatalystJpaRepository extends JpaRepository<InvestmentCatalystEntity, Long>,
        JpaSpecificationExecutor<InvestmentCatalystEntity> {
}

interface MorningNoteJpaRepository extends JpaRepository<MorningNoteEntity, Long> {
    Optional<MorningNoteEntity> findByTradeDate(LocalDate tradeDate);
}
