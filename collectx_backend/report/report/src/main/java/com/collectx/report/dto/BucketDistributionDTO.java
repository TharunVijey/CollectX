package com.collectx.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BucketDistributionDTO {
    private String name;
    private Double value;
}
