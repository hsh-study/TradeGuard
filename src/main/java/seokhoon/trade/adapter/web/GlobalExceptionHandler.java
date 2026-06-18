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
import seokhoon.trade.application.service.EarlyMarketStrategyExperimentNoDataException;
import seokhoon.trade.application.service.EarlyMarketStrategyExperimentNotFoundException;
import seokhoon.trade.application.service.LiveTradingException;
import seokhoon.trade.application.service.ResearchNotFoundException;
import seokhoon.trade.application.service.InvestorFlowDiagnosticBlockedException;
import seokhoon.trade.application.service.InvestorFlowAmountUnitUnverifiedException;
import seokhoon.trade.config.LiveTradingDisabledException;

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

    @ExceptionHandler(EarlyMarketStrategyExperimentNotFoundException.class)
    ResponseEntity<ErrorResponse> handleEarlyMarketStrategyExperimentNotFound(
            EarlyMarketStrategyExperimentNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "EARLY_MARKET_STRATEGY_EXPERIMENT_NOT_FOUND",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(EarlyMarketStrategyExperimentNoDataException.class)
    ResponseEntity<ErrorResponse> handleEarlyMarketStrategyExperimentNoData(
            EarlyMarketStrategyExperimentNoDataException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "EARLY_MARKET_STRATEGY_EXPERIMENT_NO_DATA",
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

    @ExceptionHandler({LiveTradingException.class, LiveTradingDisabledException.class})
    ResponseEntity<ErrorResponse> handleLiveTradingBlocked(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LIVE_TRADING_BLOCKED", exception.getMessage()));
    }

    @ExceptionHandler(ResearchNotFoundException.class)
    ResponseEntity<ErrorResponse> handleResearchNotFound(ResearchNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RESEARCH_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvestorFlowDiagnosticBlockedException.class)
    ResponseEntity<ErrorResponse> handleInvestorFlowDiagnosticBlocked(
            InvestorFlowDiagnosticBlockedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("INVESTOR_FLOW_DIAGNOSTIC_BLOCKED",
                        exception.getMessage()));
    }

    @ExceptionHandler(InvestorFlowAmountUnitUnverifiedException.class)
    ResponseEntity<ErrorResponse> handleInvestorFlowAmountUnitUnverified(
            InvestorFlowAmountUnitUnverifiedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("INVESTOR_FLOW_AMOUNT_UNIT_UNVERIFIED",
                        exception.getMessage()));
    }

    private static ResponseEntity<ErrorResponse> invalidRequest(String message) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", message == null ? "Invalid request" : message));
    }

    public record ErrorResponse(String code, String message) {
    }
}
