package com.collectx.payment.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class PTPResponseDTO {
    private Long ptpId;
    private Long loanAccountId;
    private Long agentId;
    private Long customerId;
    private Double promisedAmount;
    private String promisedDate;
    private String channel;
    private String promisedBy;
    private String status;
}
