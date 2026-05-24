package com.collectx.dunning.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttemptRequestDTO {
    private Long   loanAccountId;
    private Long   agentId;
    private Long   customerId;
    private String bucket;
    private String channel;
    private String outcome;
    private String notes;
    private LocalDateTime attemptTime;
}
