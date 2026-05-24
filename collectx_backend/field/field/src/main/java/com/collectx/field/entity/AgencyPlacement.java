package com.collectx.field.entity;

import com.collectx.field.enums.PlacementStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class AgencyPlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placementId;

    private Long loanAccountId;
    private Long agencyId;
    private String agencyName;         // ✅ required by frontend

    private LocalDate placementDate;   // ✅ renamed from startDate
    private Double outstandingAmount;  // ✅ required by frontend

    @Enumerated(EnumType.STRING)
    private PlacementStatus status;
}
