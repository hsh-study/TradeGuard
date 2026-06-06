package seokhoon.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import seokhoon.trade.domain.indicator.TechnicalIndicatorCalculator;
import seokhoon.trade.domain.risk.RiskManager;
import seokhoon.trade.domain.strategy.ClosingBetStrategy;

@Configuration
public class DomainConfig {
    @Bean
    TechnicalIndicatorCalculator technicalIndicatorCalculator() {
        return new TechnicalIndicatorCalculator();
    }

    @Bean
    ClosingBetStrategy closingBetStrategy() {
        return new ClosingBetStrategy();
    }

    @Bean
    RiskManager riskManager() {
        return new RiskManager();
    }
}
