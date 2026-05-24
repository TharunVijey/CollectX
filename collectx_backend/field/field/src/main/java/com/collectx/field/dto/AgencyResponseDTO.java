package com.collectx.field.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyResponseDTO {
    private Long   agencyId;
    private String name;
    private String territory;
    private Double commissionPct;
    private String status;
}
