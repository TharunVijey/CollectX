package com.collectx.portfolio.dto;

import lombok.Data;

@Data
public class LoanRequestDTO {
    // loanAccountId removed — DB auto-generates it via @GeneratedValue(IDENTITY)
    private Long customerId;
    private String product;
    private Double principalOS;
    private Double interestOS;
    private String lastPaymentDate;   // "yyyy-MM-dd"
    private String region;
}
