package com.gymplatform.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClassReportDto {
    private long totalClasses;
    private double avgAttendanceRate;
    private List<ClassPopularityDto> mostPopular;
    private List<ClassPopularityDto> leastPopular;
}
