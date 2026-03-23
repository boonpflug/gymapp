package com.gymplatform.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CheckInReportDto {
    private long totalCheckIns;
    private double avgDaily;
    private Map<Integer, Long> peakHours;    // hour of day -> count
    private Map<String, Long> peakDays;      // day of week -> count
    private List<TopCheckinMemberDto> topMembers;
}
