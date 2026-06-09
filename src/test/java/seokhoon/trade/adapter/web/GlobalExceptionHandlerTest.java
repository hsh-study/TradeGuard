package seokhoon.trade.adapter.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import seokhoon.trade.application.service.TradingSignalNotFoundException;
import seokhoon.trade.application.service.OrderRequestNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsSignalNotFoundToNotFoundResponse() {
        var response = handler.handleSignalNotFound(new TradingSignalNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("TRADING_SIGNAL_NOT_FOUND");
    }

    @Test
    void mapsIllegalArgumentToInvalidRequest() {
        var response = handler.handleInvalidRequest(new IllegalArgumentException("bad request"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("bad request");
    }

    @Test
    void mapsOrderRequestNotFoundToNotFoundResponse() {
        var response = handler.handleOrderNotFound(new OrderRequestNotFoundException(99L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("ORDER_REQUEST_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Order request not found: 99");
    }
}
