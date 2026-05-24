package com.collectx.strategy.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueueResponseDTO {
    private Long    queueId;
    private String  name;
    private String  bucketScope;
    private String  riskBand;
    private String  territory;
    private Integer capacity;
    private String  status;
}
