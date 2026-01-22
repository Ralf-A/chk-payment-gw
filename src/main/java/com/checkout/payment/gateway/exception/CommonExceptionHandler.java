package com.checkout.payment.gateway.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestControllerAdvice
public class CommonExceptionHandler {

  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<Object> handleInvalidRequest(InvalidRequestException ex) {
    ApiError error = new ApiError(
        "INVALID_REQUEST",
        ex.getMessage(),
        List.of()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(error);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
    ApiError error = new ApiError(
        "NOT_FOUND",
        ex.getMessage(),
        List.of()
    );

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(error);
  }

  @ExceptionHandler(AcquirerUnavailableException.class)
  public ResponseEntity<Object> handleAcquirerUnavailable(AcquirerUnavailableException ex) {
    ApiError error = new ApiError(
        "ACQUIRER_UNAVAILABLE",
        "Acquirer is unavailable, payment declined",
        List.of()
    );

    return ResponseEntity
        .status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneric() {
    ApiError error = new ApiError(
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        List.of()
    );

    return ResponseEntity
        .status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(error);
  }
}