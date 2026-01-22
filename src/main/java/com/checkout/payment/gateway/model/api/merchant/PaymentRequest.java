package com.checkout.payment.gateway.model.api.merchant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String cardNumber;
    private int expiryMonth;
    private int expiryYear;
    private String currency;
    private int amount;
    private String cvv;
}