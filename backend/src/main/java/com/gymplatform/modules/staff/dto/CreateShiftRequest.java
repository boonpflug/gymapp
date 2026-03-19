package com.gymplatform.modules.staff.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateShiftRequest {
    @NotNull(message = "Employee ID is required")
    private UUID employeeId;
    private UUID facilityId;
    @NotNull(message = "Start time is required")
    private Instant startTime;
    @NotNull(message = "End time is required")
    private Instant endTime;
    private String notes;
}
