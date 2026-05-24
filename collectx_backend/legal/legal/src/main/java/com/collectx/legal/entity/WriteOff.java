package com.collectx.legal.entity;

import com.collectx.legal.enums.WriteOffStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = "loan_account_id")
})
public class WriteOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long writeOffId;

    private Long loanAccountId;
    private Long customerId;      // ✅ required by frontend
    private Double writeOffAmount;// ✅ single amount field (replaces principalWO/interestWO/feesWO)
    private String reason;        // ✅ required by frontend
    private String approvedBy;    // ✅ required by frontend

    @Enumerated(EnumType.STRING)
    private WriteOffStatus status;
}
