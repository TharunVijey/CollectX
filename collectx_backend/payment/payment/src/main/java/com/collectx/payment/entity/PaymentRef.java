package com.collectx.payment.entity;

import com.collectx.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class PaymentRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private Long loanAccountId;
    private Long agentId;
    private Long customerId;

    private Double amount;
    private LocalDate paymentDate;
    private String paymentMode;
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}
