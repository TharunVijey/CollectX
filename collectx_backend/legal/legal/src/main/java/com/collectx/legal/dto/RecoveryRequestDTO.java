package com.collectx.legal.dto;

import lombok.Data;

/**
 * DTO for recording a Recovery entry.
 */
@Data
public class RecoveryRequestDTO {
    private Long loanAccountId;
    private Long customerId;
    private Double recoveredAmount;
    private String recoveryDate;   // "yyyy-MM-dd"
    private String recoveryType;   // CASH / AUCTION / SETTLEMENT / COURT_ORDER / INSURANCE
    private String notes;
    private String status;         // PENDING / COMPLETED / FAILED
}
