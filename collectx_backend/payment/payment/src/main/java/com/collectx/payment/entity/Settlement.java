package com.collectx.payment.entity;

import com.collectx.payment.enums.ApprovalStatus;
import com.collectx.payment.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    private Long loanAccountId;
    private Long agentId;
    private Long customerId;
    private Double settlementAmount;
    private Double waiverAmount;
    private String reason;
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;
}
