package seokhoon.trade.application.port.out;

public class DuplicateOrderRequestException extends RuntimeException {
    public DuplicateOrderRequestException(Throwable cause) {
        super("Duplicate order request", cause);
    }
}
