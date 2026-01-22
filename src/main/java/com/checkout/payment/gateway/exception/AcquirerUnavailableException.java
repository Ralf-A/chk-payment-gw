package com.checkout.payment.gateway.exception;

public class AcquirerUnavailableException extends RuntimeException {
  public AcquirerUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}