package com.collectx.payment.dto;

import lombok.Data;

@Data
public class SettlementRequestDTO {
    private Long loanAccountId;
    private Long agentId;
    private Long customerId;
    private Double settlementAmount;
    private Double waiverAmount;
    private String reason;
}
