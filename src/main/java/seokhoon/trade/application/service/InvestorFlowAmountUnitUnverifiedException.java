package seokhoon.trade.application.service;

public class InvestorFlowAmountUnitUnverifiedException extends RuntimeException {
    public InvestorFlowAmountUnitUnverifiedException() {
        super("KIS investor flow amount unit is unverified");
    }
}
