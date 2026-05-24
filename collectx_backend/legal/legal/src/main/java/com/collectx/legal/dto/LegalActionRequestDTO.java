package com.collectx.legal.dto;

import lombok.Data;

/**
 * DTO for filing a new Legal Action.
 * filedDate accepted as "yyyy-MM-dd" string.
 */
@Data
public class LegalActionRequestDTO {
    private Long loanAccountId;
    private Long customerId;
    private String actionType;   // NOTICE / SUMMONS / ARBITRATION / CIVIL_SUIT / CRIMINAL_COMPLAINT / LOK_ADALAT
    private String caseNumber;
    private String courtName;
    private String filedDate;    // "yyyy-MM-dd"
    private String notes;
    private String status;       // OPEN / NOTICE_SENT / HEARING / DECREE / DISPOSED
}
