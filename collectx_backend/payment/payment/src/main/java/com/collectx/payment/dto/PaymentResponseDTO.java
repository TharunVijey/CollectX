package com.collectx.payment.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class PaymentResponseDTO {
    private Long paymentId;
    private Long loanAccountId;
    private Long agentId;
    private Long customerId;
    private Double amount;
    private String paymentDate;
    private String paymentMode;
    private String referenceNumber;
    private String status;
}
