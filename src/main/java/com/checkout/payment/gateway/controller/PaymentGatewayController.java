package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.api.merchant.PaymentResponse;
import com.checkout.payment.gateway.model.api.merchant.PaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.OK)
  public PaymentResponse process(@Valid @RequestBody PaymentRequest request) {
    return paymentGatewayService.processPayment(request);
  }

  @GetMapping("/{id}")
  public PaymentResponse get(@PathVariable UUID id) {
    return paymentGatewayService.getPaymentById(id);
  }
}
