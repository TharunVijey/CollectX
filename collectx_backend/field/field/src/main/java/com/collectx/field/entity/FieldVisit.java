package com.collectx.field.entity;

import com.collectx.field.enums.VisitOutcome;
import com.collectx.field.enums.VisitType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class FieldVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long visitId;

    private Long loanAccountId;
    private Long agentId;
    private Long customerId;          // ✅ required by frontend

    private LocalDate visitDate;      // ✅ renamed from scheduledDate

    @Enumerated(EnumType.STRING)
    private VisitOutcome outcome;     // null for SCHEDULED visits (not done yet)

    @Enumerated(EnumType.STRING)
    private VisitType visitType;      // SCHEDULED (future) or COMPLETED (done)

    private String notes;
    private String address;           // ✅ renamed from geoTag
}
