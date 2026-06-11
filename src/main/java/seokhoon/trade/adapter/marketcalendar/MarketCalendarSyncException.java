package seokhoon.trade.adapter.marketcalendar;

public class MarketCalendarSyncException extends RuntimeException {
    public MarketCalendarSyncException(String message) {
        super(message);
    }

    public MarketCalendarSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
