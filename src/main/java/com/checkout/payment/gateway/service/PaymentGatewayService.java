package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.api.merchant.PaymentResponse;
import com.checkout.payment.gateway.model.api.merchant.PaymentRequest;
import java.util.UUID;

public interface PaymentGatewayService {

  PaymentResponse getPaymentById(UUID id);

  PaymentResponse processPayment(PaymentRequest paymentRequest);
}
