package com.collectx.agent.dto;

import lombok.Data;

@Data
public class HardshipRequestDTO {
    private Long   loanAccountId;
    private String reason;      // maps to HardshipReason enum
    private String startDate;
    private String endDate;
}
