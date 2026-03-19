package com.gymplatform.modules.staff.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ClockInRequest {
    @NotNull(message = "Employee ID is required")
    private UUID employeeId;
    private UUID shiftId;
    private String notes;
}
