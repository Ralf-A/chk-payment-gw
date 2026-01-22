package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.checkout.payment.gateway.model.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.CommonExceptionHandler;
import com.checkout.payment.gateway.model.api.bank.BankAcquiryRequest;
import com.checkout.payment.gateway.model.api.bank.BankAcquiryResponse;
import com.checkout.payment.gateway.model.domain.Payment;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.BankClient;
import com.checkout.payment.gateway.validation.PaymentRequestValidator;
import com.checkout.payment.gateway.service.impl.PaymentGatewayServiceImpl;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = PaymentGatewayController.class)
@Import({PaymentGatewayServiceImpl.class, CommonExceptionHandler.class})
@ActiveProfiles("test")
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  PaymentsRepository paymentsRepository;

  @MockBean
  BankClient bankClient;

  @MockBean
  PaymentRequestValidator paymentRequestValidator;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    // given
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour("4321");

    // when
    Mockito.when(paymentsRepository.findById(payment.getId()))
        .thenReturn(Optional.of(payment));

    // then
    mvc.perform(MockMvcRequestBuilders.get("/payments/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message")
            .value(org.hamcrest.Matchers.startsWith("Payment not found")));
  }

  @Test
  void whenInvalidBody_thenRejectedAnd400() throws Exception {
    String body = """
    {
      "cardNumber": "",
      "expiryMonth": 13,
      "expiryYear": 1990,
      "currency": "XXX",
      "amount": 0,
      "cvv": "1"
    }
    """;

    // validator rejects invalid body
    Mockito.doThrow(new com.checkout.payment.gateway.exception.InvalidRequestException("Invalid request"))
        .when(paymentRequestValidator)
        .validate(any());

    mvc.perform(
            MockMvcRequestBuilders.post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Rejected"));

    Mockito.verifyNoInteractions(bankClient);
  }

  @Test
  void whenExpiryInPast_thenRejected() throws Exception {
    // given: payment request with past expiry date
    YearMonth now = YearMonth.now();
    YearMonth past = now.minusMonths(1);

    String body = """
      {
        "cardNumber": "2222405343248877",
        "expiryMonth": %d,
        "expiryYear": %d,
        "currency": "GBP",
        "amount": 100,
        "cvv": "123"
      }
      """.formatted(past.getMonthValue(), past.getYear());

    Mockito.doThrow(new com.checkout.payment.gateway.exception.InvalidRequestException(
            "Card expiry date must be in the future"))
        .when(paymentRequestValidator)
        .validate(any());

    // when processing payment
    // then expect 400 with invalid request error
    mvc.perform(
            MockMvcRequestBuilders.post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Rejected"));

    Mockito.verifyNoInteractions(bankClient);
  }

  @Test
  void whenUnsupportedCurrency_thenRejected() throws Exception {
    // given: payment request with unsupported currency
    String body = """
    {
      "cardNumber": "2222405343248877",
      "expiryMonth": 12,
      "expiryYear": 2099,
      "currency": "JPY",
      "amount": 100,
      "cvv": "123"
    }
    """;

    Mockito.doThrow(new com.checkout.payment.gateway.exception.InvalidRequestException("Unsupported currency"))
        .when(paymentRequestValidator)
        .validate(any());

    // when processing payment
    // then expect 400 with validation errors
    mvc.perform(
            MockMvcRequestBuilders.post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Rejected"));

    Mockito.verifyNoInteractions(bankClient);
  }

  @Test
  void whenCardEndsOdd_thenAuthorized() throws Exception {
    // given + when: acquirer will authorize payment
    when(bankClient.charge(any(BankAcquiryRequest.class)))
        .thenReturn(new BankAcquiryResponse(true, "AUTH_CODE"));

    String body = """
        {
          "cardNumber": "2222405343248877",
          "expiryMonth": 12,
          "expiryYear": 2099,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """;
    // then: payment is authorized
    mvc.perform(
            MockMvcRequestBuilders.post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value("8877"));
  }

  @Test
  void whenCardEndsEven_thenDeclined() throws Exception {
    // given+when: acquirer declining payment
    when(bankClient.charge(any(BankAcquiryRequest.class)))
        .thenReturn(new BankAcquiryResponse(false, "AUTH_CODE"));

    String body = """
        {
          "cardNumber": "2222405343248876",
          "expiryMonth": 12,
          "expiryYear": 2099,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """;

    // then: payment is declined
    mvc.perform(
            MockMvcRequestBuilders.post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()));
  }

  @Test
  void whenCardEndsZero_thenAcquirerUnavailableDeclined() throws Exception {
    // given+when: acquirer is unavailable
    when(bankClient.charge(any(BankAcquiryRequest.class)))
        .thenThrow(new com.checkout.payment.gateway.exception.AcquirerUnavailableException("Acquirer unavailable", null));

    String body = """
        {
          "cardNumber": "2222405343248870",
          "expiryMonth": 12,
          "expiryYear": 2099,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """;

    // then: payment is declined
    mvc.perform(
            MockMvcRequestBuilders.post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()));
  }

  @Test
  void whenCardEndsWithZero_then503() throws Exception {
    // given
    String body = """
      {
        "cardNumber": "2222405343248870",
        "expiryMonth": 12,
        "expiryYear": 2099,
        "currency": "GBP",
        "amount": 100,
        "cvv": "123"
      }
      """;

    // when+then: acquirer returns 503
    mvc.perform(
            MockMvcRequestBuilders.post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
  }
}
