package com.gymplatform.modules.staff.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TimeEntryDto {
    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private UUID shiftId;
    private Instant clockIn;
    private Instant clockOut;
    private Integer breakMinutes;
    private Integer totalMinutes;
    private String notes;
    private Instant createdAt;
}
