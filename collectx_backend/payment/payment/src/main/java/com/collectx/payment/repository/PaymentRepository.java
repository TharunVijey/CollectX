package com.collectx.payment.repository;

import com.collectx.payment.entity.PaymentRef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentRef, Long> {
}