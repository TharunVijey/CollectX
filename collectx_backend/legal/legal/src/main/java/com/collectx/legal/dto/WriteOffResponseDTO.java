package com.collectx.legal.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO returned for Write-Off responses.
 */
@Data
@Builder
public class WriteOffResponseDTO {
    private Long writeOffId;
    private Long loanAccountId;
    private Long customerId;
    private Double writeOffAmount;
    private String reason;
    private String approvedBy;
    private String status;
}
