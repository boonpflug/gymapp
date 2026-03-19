package com.gymplatform.modules.sales.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SalesPipelineDto {
    private List<LeadStageDto> stages;
    private long totalLeads;
    private long convertedLeads;
    private double conversionRate;
}
