package com.gymplatform.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassPopularityDto {
    private String className;
    private long totalBookings;
    private double avgAttendance;
    private double capacityUtilization;
}
