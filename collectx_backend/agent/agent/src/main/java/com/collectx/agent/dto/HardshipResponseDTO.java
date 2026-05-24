package com.collectx.agent.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HardshipResponseDTO {
    private Long   hardshipId;
    private Long   loanAccountId;
    private String reason;
    private String startDate;
    private String endDate;
    private String status;
}
