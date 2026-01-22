package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.api.bank.BankAcquiryRequest;
import com.checkout.payment.gateway.model.api.bank.BankAcquiryResponse;

public interface BankClient {
  BankAcquiryResponse charge(BankAcquiryRequest request);
}