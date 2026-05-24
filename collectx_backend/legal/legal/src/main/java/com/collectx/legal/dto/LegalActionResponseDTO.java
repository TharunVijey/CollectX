package com.collectx.legal.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO returned for Legal Action responses.
 */
@Data
@Builder
public class LegalActionResponseDTO {
    private Long legalActionId;
    private Long loanAccountId;
    private Long customerId;
    private String actionType;
    private String caseNumber;
    private String courtName;
    private String filedDate;
    private String status;
    private String notes;
}
