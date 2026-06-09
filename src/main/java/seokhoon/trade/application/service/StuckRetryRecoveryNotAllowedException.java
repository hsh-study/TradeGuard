package seokhoon.trade.application.service;

public class StuckRetryRecoveryNotAllowedException extends IllegalArgumentException {
    public StuckRetryRecoveryNotAllowedException(String message) {
        super(message);
    }
}
