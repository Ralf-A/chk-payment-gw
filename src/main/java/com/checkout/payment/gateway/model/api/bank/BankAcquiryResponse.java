package com.checkout.payment.gateway.model.api.bank;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAcquiryResponse {
    private boolean authorized;
    @JsonProperty("authorization_code")
    private String authorizationCode;
}