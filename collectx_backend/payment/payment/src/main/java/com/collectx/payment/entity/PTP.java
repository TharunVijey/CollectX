package com.collectx.payment.entity;

import com.collectx.payment.enums.PTPStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class PTP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ptpId;

    private Long loanAccountId;
    private Long agentId;
    private Long customerId;

    private Double promisedAmount;
    private LocalDate promisedDate;
    private String channel;
    private String promisedBy;

    @Enumerated(EnumType.STRING)
    private PTPStatus status;
}
