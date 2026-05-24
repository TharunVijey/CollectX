package com.collectx.legal.entity;

import com.collectx.legal.enums.RecoveryStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Recovery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recoveryId;

    private Long loanAccountId;
    private Long customerId;       // ✅ required by frontend
    private Double recoveredAmount;// ✅ renamed from amount
    private String recoveryType;   // ✅ renamed from source (was enum, now String: CASH/AUCTION/etc.)
    private LocalDate recoveryDate;
    private String notes;

    @Enumerated(EnumType.STRING)
    private RecoveryStatus status;
}
