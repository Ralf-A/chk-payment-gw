package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.exception.InvalidRequestException;
import com.checkout.payment.gateway.model.api.merchant.PaymentRequest;
import java.time.YearMonth;
import com.checkout.payment.gateway.validation.impl.PaymentRequestValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRequestValidatorTest {

  private PaymentRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PaymentRequestValidatorImpl();
  }

  @Test
  void whenValidRequest_thenNoException() {
    YearMonth future = YearMonth.now().plusMonths(1);

    PaymentRequest req = new PaymentRequest(
        "2222405343248877",
        future.getMonthValue(),
        future.getYear(),
        "GBP",
        100,
        "123"
    );

    assertThatCode(() -> validator.validate(req))
        .doesNotThrowAnyException();
  }

  @Test
  void whenNullRequest_thenInvalidRequestException() {
    assertThatThrownBy(() -> validator.validate(null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("must not be null");
  }

  @Test
  void whenInvalidCardNumber_thenInvalidRequestException() {
    YearMonth future = YearMonth.now().plusMonths(1);

    PaymentRequest req = new PaymentRequest(
        "123", // too short
        future.getMonthValue(),
        future.getYear(),
        "GBP",
        100,
        "123"
    );

    assertThatThrownBy(() -> validator.validate(req))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Invalid card number");
  }

  @Test
  void whenExpiryInPast_thenInvalidRequestException() {
    YearMonth past = YearMonth.now().minusMonths(1);

    PaymentRequest req = new PaymentRequest(
        "2222405343248877",
        past.getMonthValue(),
        past.getYear(),
        "GBP",
        100,
        "123"
    );

    // didn't check for log as could be flaky depending on current date
    // (2026-1 vs 2026-2 will have diff response from validator as I made it)
    assertThatThrownBy(() -> validator.validate(req))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void whenUnsupportedCurrency_thenInvalidRequestException() {
    YearMonth future = YearMonth.now().plusMonths(1);

    PaymentRequest req = new PaymentRequest(
        "2222405343248877",
        future.getMonthValue(),
        future.getYear(),
        "JPY", // unsupported
        100,
        "123"
    );

    assertThatThrownBy(() -> validator.validate(req))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Currency must be one of GBP, USD, or EUR");
  }
}
