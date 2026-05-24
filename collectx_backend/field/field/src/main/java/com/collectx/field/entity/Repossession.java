package com.collectx.field.entity;

import com.collectx.field.enums.RepossessionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Repossession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long repossessionId;

    private Long loanAccountId;
    private Long agentId;              // ✅ required by frontend

    private String assetDescription;   // ✅ renamed from assetDetailsJson
    private LocalDate repossessedDate; // ✅ renamed from initiatedDate
    private Double estimatedValue;     // ✅ renamed from realizationAmount

    @Enumerated(EnumType.STRING)
    private RepossessionStatus status; // ✅ renamed from actionStatus
}
