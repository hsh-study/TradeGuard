package seokhoon.trade.adapter.marketdata.kis;

public class KisApiException extends RuntimeException {
    public KisApiException(String message) {
        super(message);
    }

    public KisApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
