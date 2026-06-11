package seokhoon.trade.adapter.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import seokhoon.trade.application.service.TradingSignalNotFoundException;
import seokhoon.trade.application.service.OrderRequestNotFoundException;
import seokhoon.trade.application.service.EarlyMarketPerformanceNotFoundException;
import seokhoon.trade.application.service.EarlyMarketFollowUpResultNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TradingSignalNotFoundException.class)
    ResponseEntity<ErrorResponse> handleSignalNotFound(TradingSignalNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("TRADING_SIGNAL_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(OrderRequestNotFoundException.class)
    ResponseEntity<ErrorResponse> handleOrderNotFound(OrderRequestNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ORDER_REQUEST_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(EarlyMarketPerformanceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleEarlyMarketPerformanceNotFound(
            EarlyMarketPerformanceNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "EARLY_MARKET_PERFORMANCE_NOT_FOUND",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(EarlyMarketFollowUpResultNotFoundException.class)
    ResponseEntity<ErrorResponse> handleEarlyMarketFollowUpResultNotFound(
            EarlyMarketFollowUpResultNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "EARLY_MARKET_FOLLOW_UP_RESULT_NOT_FOUND",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request");
        return invalidRequest(message);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    ResponseEntity<ErrorResponse> handleInvalidRequest(Exception exception) {
        return invalidRequest(exception.getMessage());
    }

    private static ResponseEntity<ErrorResponse> invalidRequest(String message) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", message == null ? "Invalid request" : message));
    }

    public record ErrorResponse(String code, String message) {
    }
}
