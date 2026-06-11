package seokhoon.trade.application.service;

public class EarlyMarketFollowUpResultNotFoundException
        extends RuntimeException {
    public EarlyMarketFollowUpResultNotFoundException(long signalId) {
        super("Early market follow-up result not found: " + signalId);
    }
}
