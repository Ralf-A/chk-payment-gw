package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.api.merchant.PaymentRequest;

public interface PaymentRequestValidator {

  /**
   * Validates a merchant payment request at the gateway level.
   * Implementation throws an exception on invalid data so that
   * the caller can mark the payment as `Rejected` without calling the acquirer.
   *
   * @param request the request to validate
   * @throws com.checkout.payment.gateway.exception.InvalidRequestException when invalid
   */
  void validate(PaymentRequest request);
}
