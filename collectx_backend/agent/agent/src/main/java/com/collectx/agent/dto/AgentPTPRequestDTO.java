package com.collectx.agent.dto;

import lombok.Data;

@Data
public class AgentPTPRequestDTO {
    private Long loanAccountId;
    private Double amount;
}
