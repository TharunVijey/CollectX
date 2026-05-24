package com.collectx.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CashFlowDTO {
    private String month;
    private Double collected;
    private Double target;
}
