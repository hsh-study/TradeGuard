package seokhoon.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import seokhoon.trade.domain.order.OrderType;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "tradeguard.live-trading")
public class LiveTradingProperties {
    private boolean liveTradingEnabled;
    private boolean kisTradingEnabled;
    private String accountNumber = "";
    private String accountProductCode = "";
    private String tradingBaseUrl = "https://openapi.koreainvestment.com:9443";
    private String kisEnvironment = "REAL";
    private BigDecimal buyCommissionRate = new BigDecimal("0.00015");
    private BigDecimal sellCommissionRate = new BigDecimal("0.00015");
    private BigDecimal sellTaxRate = new BigDecimal("0.0020");
    private BigDecimal minimumNetProfitAmount = BigDecimal.ZERO;
    private BigDecimal maxLossAmountPerPosition = new BigDecimal("30000");
    private BigDecimal defaultTakeProfitRate = new BigDecimal("5.0");
    private BigDecimal defaultStopLossRate = new BigDecimal("-3.0");
    private BigDecimal highPriceThreshold1 = new BigDecimal("50000");
    private BigDecimal highPriceThreshold2 = new BigDecimal("200000");
    private BigDecimal maxAllowedOrderAmount = new BigDecimal("1000000");
    private OrderType allowedOrderType = OrderType.LIMIT;
    private boolean liveOrderAutoCancelEnabled;
    private int buyOrderExpireMinutes = 3;
    private int sellOrderExpireMinutes = 3;
    private int cancelBeforeMarketCloseMinutes = 5;

    public void validateConfiguration() {
        nonNegative(buyCommissionRate, "buyCommissionRate");
        nonNegative(sellCommissionRate, "sellCommissionRate");
        nonNegative(sellTaxRate, "sellTaxRate");
        nonNegative(defaultTakeProfitRate, "defaultTakeProfitRate");
        if (defaultStopLossRate == null || defaultStopLossRate.signum() > 0) {
            throw new IllegalStateException("defaultStopLossRate must be zero or negative");
        }
        if (maxAllowedOrderAmount == null || maxAllowedOrderAmount.signum() <= 0) {
            throw new IllegalStateException("maxAllowedOrderAmount must be positive");
        }
        if (allowedOrderType != OrderType.LIMIT) {
            throw new IllegalStateException("Only LIMIT live orders are allowed");
        }
        if (buyOrderExpireMinutes <= 0 || sellOrderExpireMinutes <= 0) {
            throw new IllegalStateException("Live order expiration minutes must be positive");
        }
        if (cancelBeforeMarketCloseMinutes < 0) {
            throw new IllegalStateException("cancelBeforeMarketCloseMinutes must be non-negative");
        }
    }

    public void validateOrderEnabled() {
        validateConfiguration();
        if (!liveTradingEnabled) {
            throw new LiveTradingDisabledException("LIVE_TRADING_ENABLED is false");
        }
        validateKisAccessEnabled();
    }

    public void validateKisAccessEnabled() {
        validateConfiguration();
        if (!kisTradingEnabled) {
            throw new LiveTradingDisabledException("KIS_TRADING_ENABLED is false");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new LiveTradingDisabledException("KIS account number is not configured");
        }
        if (accountProductCode == null || accountProductCode.isBlank()) {
            throw new LiveTradingDisabledException("KIS account product code is not configured");
        }
        if ("REAL".equalsIgnoreCase(kisEnvironment)) {
            if (!"https://openapi.koreainvestment.com:9443".equals(tradingBaseUrl)) {
                throw new LiveTradingDisabledException("REAL trading requires the KIS production host");
            }
        } else if ("DEMO".equalsIgnoreCase(kisEnvironment)) {
            if (!"https://openapivts.koreainvestment.com:29443".equals(tradingBaseUrl)) {
                throw new LiveTradingDisabledException("DEMO trading requires the KIS virtual host");
            }
        } else {
            throw new LiveTradingDisabledException("KIS trading environment must be REAL or DEMO");
        }
    }

    private static void nonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalStateException(name + " must be zero or positive");
        }
    }

    public boolean isLiveTradingEnabled() { return liveTradingEnabled; }
    public void setLiveTradingEnabled(boolean value) { liveTradingEnabled = value; }
    public boolean isKisTradingEnabled() { return kisTradingEnabled; }
    public void setKisTradingEnabled(boolean value) { kisTradingEnabled = value; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String value) { accountNumber = value; }
    public String getAccountProductCode() { return accountProductCode; }
    public void setAccountProductCode(String value) { accountProductCode = value; }
    public String getTradingBaseUrl() { return tradingBaseUrl; }
    public void setTradingBaseUrl(String value) { tradingBaseUrl = value; }
    public String getKisEnvironment() { return kisEnvironment; }
    public void setKisEnvironment(String value) { kisEnvironment = value; }
    public BigDecimal getBuyCommissionRate() { return buyCommissionRate; }
    public void setBuyCommissionRate(BigDecimal value) { buyCommissionRate = value; }
    public BigDecimal getSellCommissionRate() { return sellCommissionRate; }
    public void setSellCommissionRate(BigDecimal value) { sellCommissionRate = value; }
    public BigDecimal getSellTaxRate() { return sellTaxRate; }
    public void setSellTaxRate(BigDecimal value) { sellTaxRate = value; }
    public BigDecimal getMinimumNetProfitAmount() { return minimumNetProfitAmount; }
    public void setMinimumNetProfitAmount(BigDecimal value) { minimumNetProfitAmount = value; }
    public BigDecimal getMaxLossAmountPerPosition() { return maxLossAmountPerPosition; }
    public void setMaxLossAmountPerPosition(BigDecimal value) { maxLossAmountPerPosition = value; }
    public BigDecimal getDefaultTakeProfitRate() { return defaultTakeProfitRate; }
    public void setDefaultTakeProfitRate(BigDecimal value) { defaultTakeProfitRate = value; }
    public BigDecimal getDefaultStopLossRate() { return defaultStopLossRate; }
    public void setDefaultStopLossRate(BigDecimal value) { defaultStopLossRate = value; }
    public BigDecimal getHighPriceThreshold1() { return highPriceThreshold1; }
    public void setHighPriceThreshold1(BigDecimal value) { highPriceThreshold1 = value; }
    public BigDecimal getHighPriceThreshold2() { return highPriceThreshold2; }
    public void setHighPriceThreshold2(BigDecimal value) { highPriceThreshold2 = value; }
    public BigDecimal getMaxAllowedOrderAmount() { return maxAllowedOrderAmount; }
    public void setMaxAllowedOrderAmount(BigDecimal value) { maxAllowedOrderAmount = value; }
    public OrderType getAllowedOrderType() { return allowedOrderType; }
    public void setAllowedOrderType(OrderType value) { allowedOrderType = value; }
    public boolean isLiveOrderAutoCancelEnabled() { return liveOrderAutoCancelEnabled; }
    public void setLiveOrderAutoCancelEnabled(boolean value) { liveOrderAutoCancelEnabled = value; }
    public int getBuyOrderExpireMinutes() { return buyOrderExpireMinutes; }
    public void setBuyOrderExpireMinutes(int value) { buyOrderExpireMinutes = value; }
    public int getSellOrderExpireMinutes() { return sellOrderExpireMinutes; }
    public void setSellOrderExpireMinutes(int value) { sellOrderExpireMinutes = value; }
    public int getCancelBeforeMarketCloseMinutes() { return cancelBeforeMarketCloseMinutes; }
    public void setCancelBeforeMarketCloseMinutes(int value) { cancelBeforeMarketCloseMinutes = value; }
}
