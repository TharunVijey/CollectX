package com.collectx.legal.dto;

import lombok.Data;

/**
 * DTO for recording a Write-Off (ADMIN only).
 */
@Data
public class WriteOffRequestDTO {
    private Long loanAccountId;
    private Long customerId;
    private Double writeOffAmount;
    private String reason;
    private String approvedBy;
    private String status;       // DRAFT / PENDING / POSTED / REVERSED
}
