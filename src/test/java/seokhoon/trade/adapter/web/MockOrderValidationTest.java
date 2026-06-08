package seokhoon.trade.adapter.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import seokhoon.trade.domain.strategy.SignalType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MockOrderValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsQuantityZeroForLogicalKeyMockOrderRequest() {
        MockOrderController.MockOrderRequest request = new MockOrderController.MockOrderRequest(
                "CLOSING_BET",
                "005930",
                LocalDate.of(2026, 6, 5),
                SignalType.BUY_CANDIDATE,
                0,
                BigDecimal.valueOf(50_000)
        );

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("quantity"));
    }

    @Test
    void rejectsNullLimitPriceForSignalIdMockOrderRequest() {
        SignalController.SignalMockOrderRequest request = new SignalController.SignalMockOrderRequest(1, null);

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("limitPrice"));
    }

    @Test
    void rejectsNegativeLimitPriceForSignalIdMockOrderRequest() {
        SignalController.SignalMockOrderRequest request = new SignalController.SignalMockOrderRequest(
                1,
                BigDecimal.valueOf(-1)
        );

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("limitPrice"));
    }
}
