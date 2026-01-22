package com.checkout.payment.gateway.model.api.merchant;

import com.checkout.payment.gateway.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private UUID id;
    private PaymentStatus status;
    private String cardNumberLastFour;
    private int expiryMonth;
    private int expiryYear;
    private String currency;
    private int amount;
}