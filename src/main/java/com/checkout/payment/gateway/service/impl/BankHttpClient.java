package com.checkout.payment.gateway.service.impl;

import com.checkout.payment.gateway.exception.AcquirerUnavailableException;
import com.checkout.payment.gateway.exception.InvalidRequestException;
import com.checkout.payment.gateway.model.api.bank.BankAcquiryRequest;
import com.checkout.payment.gateway.model.api.bank.BankAcquiryResponse;
import com.checkout.payment.gateway.service.BankClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class BankHttpClient implements BankClient {
  private final WebClient webClient;

  public BankHttpClient(
      WebClient.Builder builder,
      @Value("${acquirer.url}") String acquirerUrl) {

    this.webClient = builder
        .baseUrl(acquirerUrl)
        .build();
  }

  @Override
  public BankAcquiryResponse charge(BankAcquiryRequest request) {
    log.info("Request to acquirer: amount={} currency={}", request.getAmount(), request.getCurrency());
    try {
      BankAcquiryResponse response = webClient.post()
          .uri("/payments")
          .bodyValue(request)
          .retrieve()
          .bodyToMono(BankAcquiryResponse.class)
          .block();

      assert response != null;
      log.info("Received response from acquirer: response={}", response.getAuthorizationCode());
      return response;

    } catch (WebClientResponseException e) {
      if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
        throw new AcquirerUnavailableException("Acquirer unavailable", null);
      }

      log.error("HTTP error from acquirer: status={} body={}",
          e.getStatusCode(), e.getResponseBodyAsString(), e);

      if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
        throw new InvalidRequestException(
            "Acquirer rejected request: " + e.getResponseBodyAsString());
      }
      throw e;

    } catch (WebClientRequestException e) {
      log.error("Failed to connect to acquirer: {}", e.getMessage(), e);
      throw new AcquirerUnavailableException("Failed to connect to acquirer", e);
    }
  }
}