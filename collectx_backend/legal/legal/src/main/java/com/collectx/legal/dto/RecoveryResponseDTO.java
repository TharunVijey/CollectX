package com.collectx.legal.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO returned for Recovery responses.
 */
@Data
@Builder
public class RecoveryResponseDTO {
    private Long recoveryId;
    private Long loanAccountId;
    private Long customerId;
    private Double recoveredAmount;
    private String recoveryType;
    private String recoveryDate;
    private String notes;
}
