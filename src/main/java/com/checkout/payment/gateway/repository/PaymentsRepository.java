package com.checkout.payment.gateway.repository;

import java.util.UUID;
import com.checkout.payment.gateway.model.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentsRepository extends JpaRepository<Payment, UUID> {
}