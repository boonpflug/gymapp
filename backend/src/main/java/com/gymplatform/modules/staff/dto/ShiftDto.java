package com.gymplatform.modules.staff.dto;

import com.gymplatform.modules.staff.ShiftStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ShiftDto {
    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private UUID facilityId;
    private Instant startTime;
    private Instant endTime;
    private ShiftStatus status;
    private String notes;
    private long durationMinutes;
    private Instant createdAt;
}
