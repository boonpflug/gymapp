package com.gymplatform.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class MemberReportDto {
    private long totalActive;
    private long totalInactive;
    private long newThisMonth;
    private double churnRate;
    private Map<String, Long> cancellationReasons;
}
