package com.checkout.payment.gateway.validation.impl;

import com.checkout.payment.gateway.exception.InvalidRequestException;
import com.checkout.payment.gateway.model.api.merchant.PaymentRequest;
import java.time.YearMonth;

import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestValidatorImpl implements PaymentRequestValidator {

  @Override
  public void validate(PaymentRequest req) {
    if (req == null) {
      throw new InvalidRequestException("Request must not be null");
    }

    if (req.getCardNumber() == null
        || !req.getCardNumber().matches("\\d{14,19}")) {
      throw new InvalidRequestException("Invalid card number");
    }

    if (req.getCvv() == null
        || !req.getCvv().matches("\\d{3,4}")) {
      throw new InvalidRequestException("Invalid CVV");
    }

    if (req.getAmount() <= 0) {
      throw new InvalidRequestException("Amount must be greater than zero");
    }

    if (req.getCurrency() == null
        || !req.getCurrency().matches("GBP|USD|EUR")) {
      throw new InvalidRequestException("Currency must be one of GBP, USD, or EUR");
    }

    validateExpiryDate(req);
  }

  private void validateExpiryDate(PaymentRequest req) {
    int month = req.getExpiryMonth();
    int year = req.getExpiryYear();

    if (month < 1 || month > 12) {
      throw new InvalidRequestException("Expiry month must be between 1 and 12");
    }
    if (year < YearMonth.now().getYear()) {
      throw new InvalidRequestException("Card expiry year must not be in the past");
    }

    YearMonth expiry = YearMonth.of(year, month);
    YearMonth now = YearMonth.now();
    if (expiry.isBefore(now)) {
      throw new InvalidRequestException("Card expiry date must be in the future");
    }
  }
}