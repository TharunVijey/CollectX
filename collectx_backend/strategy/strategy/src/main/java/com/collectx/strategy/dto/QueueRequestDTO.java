package com.collectx.strategy.dto;

import lombok.Data;

@Data
public class QueueRequestDTO {
    private String name;
    private String bucketScope;
    private String riskBand;
    private String territory;
    private Integer capacity;
}
