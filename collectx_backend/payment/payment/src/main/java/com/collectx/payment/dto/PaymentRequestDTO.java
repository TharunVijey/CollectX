package com.collectx.payment.dto;

import lombok.Data;


@Data
public class PaymentRequestDTO {
    private Long loanAccountId;
    private Long agentId;
    private Long customerId;
    private Double amount;
    private String paymentMode;
    private String referenceNumber;
}
