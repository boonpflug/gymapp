package com.gymplatform.modules.staff.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ShiftReportDto {
    private UUID employeeId;
    private String employeeName;
    private int scheduledMinutes;
    private int actualMinutes;
    private int difference;
    private int shiftCount;
    private int timeEntryCount;
}
