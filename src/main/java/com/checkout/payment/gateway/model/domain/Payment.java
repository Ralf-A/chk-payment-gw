package com.checkout.payment.gateway.model.domain;

import com.checkout.payment.gateway.model.enums.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Setter
@Table(name = "payments")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String cardNumberLastFour;
    private int expiryMonth;
    private int expiryYear;
    private String currency;
    private int amount;
}