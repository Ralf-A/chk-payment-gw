package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquirerUnavailableException;
import com.checkout.payment.gateway.exception.InvalidRequestException;
import com.checkout.payment.gateway.model.api.bank.BankAcquiryResponse;
import com.checkout.payment.gateway.model.api.merchant.PaymentRequest;
import com.checkout.payment.gateway.model.api.merchant.PaymentResponse;
import com.checkout.payment.gateway.model.domain.Payment;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.impl.PaymentGatewayServiceImpl;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import org.junit.jupiter.api.*;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

public class PaymentGatewayServiceTest {

  PaymentsRepository repo;
  BankClient acquirer;
  PaymentGatewayService service;
  PaymentRequestValidator validator;

  @BeforeEach
  void setUp() {
    repo = mock(PaymentsRepository.class);
    acquirer = mock(BankClient.class);
    validator = mock(PaymentRequestValidator.class);
    service = new PaymentGatewayServiceImpl(repo, acquirer, validator);
  }

  @Test
  void whenAcquirerAuthorizes_paymentAuthorized() {
    // given: card ends with an odd number
    when(acquirer.charge(any())).thenReturn(new BankAcquiryResponse(true, "abc"));

    // when: processing a payment
    PaymentResponse resp = service.processPayment(validReq("2222405343248877"));

    // then: payment is authorized
    assertThat(resp.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(resp.getCardNumberLastFour()).isEqualTo("8877");
  }

  @Test
  void whenAcquirerDeclines_paymentDeclined() {
    // given: card ends with an even number
    when(acquirer.charge(any())).thenReturn(new BankAcquiryResponse(false, null));

    // when: processing a payment
    PaymentResponse resp = service.processPayment(validReq("2222405343248876"));

    // then: payment is declined
    assertThat(resp.getStatus()).isEqualTo(PaymentStatus.DECLINED);
  }

  @Test
  void whenAcquirerUnavailable_paymentDeclined() {
    // given: card ends with a zero
    when(acquirer.charge(any())).thenThrow(new AcquirerUnavailableException("Acquirer unavailable", null));

    // when: processing a payment
    PaymentResponse resp = service.processPayment(validReq("2222405343248870"));

    // then: payment is declined
    assertThat(resp.getStatus()).isEqualTo(PaymentStatus.DECLINED);
  }

  @Test
  void whenExpiryDateInPast_paymentRejected() {
    // given: a payment request with past expiry date
    YearMonth now = YearMonth.now();
    YearMonth past = now.minusMonths(1);

    PaymentRequest req = new PaymentRequest(
        "2222405343248877",
        past.getMonthValue(),
        past.getYear(),
        "GBP",
        100,
        "123"
    );

    // validator rejects the request
    doThrow(new InvalidRequestException("Card expiry date must be in the future"))
        .when(validator).validate(req);

    // when
    PaymentResponse resp = service.processPayment(req);

    // then: payment is rejected and bank is not called
    assertThat(resp.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    verifyNoInteractions(acquirer);
  }

  // java
  @Test
  void getPaymentById_returnsPayment() {
    // given
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("GBP");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2099);
    payment.setCardNumberLastFour("8877");

    when(repo.findById(payment.getId())).thenReturn(Optional.of(payment));

    // when
    PaymentResponse resp = service.getPaymentById(payment.getId());

    // then
    assertThat(resp.getId()).isEqualTo(payment.getId());
    assertThat(resp.getAmount()).isEqualTo(payment.getAmount());
    assertThat(resp.getCurrency()).isEqualTo(payment.getCurrency());
    assertThat(resp.getStatus()).isEqualTo(payment.getStatus());
    assertThat(resp.getCardNumberLastFour()).isEqualTo(payment.getCardNumberLastFour());
    assertThat(resp.getExpiryMonth()).isEqualTo(payment.getExpiryMonth());
    assertThat(resp.getExpiryYear()).isEqualTo(payment.getExpiryYear());
  }

  @Test
  void getPaymentById_notFound() {
    // given
    UUID id = UUID.randomUUID();
    when(repo.findById(id)).thenReturn(Optional.empty());

    // when + then
    assertThatThrownBy(() -> service.getPaymentById(id))
        .isInstanceOf(com.checkout.payment.gateway.exception.NotFoundException.class)
        .hasMessageContaining("Payment not found");
  }

  private PaymentRequest validReq(String pan) {
    return new PaymentRequest(pan, 12, 2099, "GBP", 100, "123");
  }
}

