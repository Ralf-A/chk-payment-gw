package com.checkout.payment.gateway.service.impl;

import com.checkout.payment.gateway.model.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquirerUnavailableException;
import com.checkout.payment.gateway.exception.InvalidRequestException;
import com.checkout.payment.gateway.exception.NotFoundException;
import com.checkout.payment.gateway.model.api.bank.BankAcquiryRequest;
import com.checkout.payment.gateway.model.api.bank.BankAcquiryResponse;
import com.checkout.payment.gateway.model.api.merchant.PaymentRequest;
import com.checkout.payment.gateway.model.api.merchant.PaymentResponse;
import com.checkout.payment.gateway.model.domain.Payment;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import com.checkout.payment.gateway.service.BankClient;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

  private final PaymentsRepository repo;
  private final BankClient acquirer;
  private final PaymentRequestValidator validator;

  public PaymentGatewayServiceImpl(PaymentsRepository repo, BankClient acquirer, PaymentRequestValidator validator) {
    this.repo = repo;
    this.acquirer = acquirer;
    this.validator = validator;
  }

  @Override
  public PaymentResponse getPaymentById(UUID id) {
    log.info("Retrieving payment with id={}", id);
    return repo.findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new NotFoundException("Payment not found: " + id));
  }


  @Override
  public PaymentResponse processPayment(PaymentRequest req) {
    UUID paymentId = UUID.randomUUID();
    try {
      validator.validate(req);
    } catch (InvalidRequestException e) {
      log.warn("Rejecting payment id={} due to invalid request: {}", paymentId, e.getMessage());
      return new PaymentResponse(
          paymentId,
          PaymentStatus.REJECTED,
          null,
          req.getExpiryMonth(),
          req.getExpiryYear(),
          req.getCurrency(),
          req.getAmount()
      );
    }

    PaymentStatus status;
    try {
      log.info("Processing payment id={} currency={} amount={}",
          paymentId, req.getCurrency(), req.getAmount());

      BankAcquiryRequest acquirerReq = new BankAcquiryRequest(
          req.getCardNumber(),
          "%02d/%d".formatted(req.getExpiryMonth(), req.getExpiryYear()),
          req.getCurrency(),
          req.getAmount(),
          req.getCvv()
      );

      BankAcquiryResponse acqResp = acquirer.charge(acquirerReq);
      status = acqResp.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
      log.info("Acquirer result for id={}: {}", paymentId, status.getName());

    } catch (AcquirerUnavailableException e) {
      status = PaymentStatus.DECLINED;
      log.warn("Acquirer unavailable for id={}, marking as Declined", paymentId, e);
    }

    String lastFourCardDigits = lastFourDigits(req.getCardNumber());

    Payment payment = new Payment(
        paymentId,
        status,
        lastFourCardDigits,
        req.getExpiryMonth(),
        req.getExpiryYear(),
        req.getCurrency(),
        req.getAmount()
    );

    repo.save(payment);
    log.info("Persisted payment id={} with status={}", paymentId, status.getName());

    return toResponse(payment);
  }

  private PaymentResponse toResponse(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getStatus(),
        payment.getCardNumberLastFour(),
        payment.getExpiryMonth(),
        payment.getExpiryYear(),
        payment.getCurrency(),
        payment.getAmount()
    );
  }

  private String lastFourDigits(String cardNumber) {
    return cardNumber.substring(cardNumber.length() - 4);
  }
}