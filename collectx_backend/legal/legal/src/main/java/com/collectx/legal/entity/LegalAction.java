package com.collectx.legal.entity;

import com.collectx.legal.enums.LegalStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class LegalAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long legalId;   // maps to existing DB column `legal_id` — do NOT rename

    private Long loanAccountId;
    private Long customerId;
    private String caseNumber;
    private String courtName;

    // ✅ Stored as plain String — avoids MySQL ENUM column truncation issue.
    // Valid values: NOTICE / SUMMONS / ARBITRATION / CIVIL_SUIT / CRIMINAL_COMPLAINT / LOK_ADALAT
    @Column(length = 50)
    private String actionType;

    private LocalDate filedDate;

    @Enumerated(EnumType.STRING)
    private LegalStatus status;

    private String notes;
}
