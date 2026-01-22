package com.checkout.payment.gateway.model.api.bank;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BankAcquiryRequest {
    @JsonProperty("card_number")
    private String cardNumber;
    @JsonProperty("expiry_date")
    private String expiryDate;
    private String currency;
    private int amount;
    private String cvv;
}