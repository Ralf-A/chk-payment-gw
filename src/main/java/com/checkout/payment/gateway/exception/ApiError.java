package com.checkout.payment.gateway.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class ApiError {

  private final Instant timestamp = Instant.now();
  private final String code;
  private final String message;
  private final List<FieldError> fieldErrors;

  public record FieldError(String field, String message) {}
}
