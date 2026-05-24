package com.collectx.payment.dto;

import lombok.Data;


@Data
public class PTPRequestDTO {
    private Long loanAccountId;
    private Long agentId;
    private Long customerId;
    private Double promisedAmount;
    private String promisedDate;
    private String channel;
    private String promisedBy;
}
