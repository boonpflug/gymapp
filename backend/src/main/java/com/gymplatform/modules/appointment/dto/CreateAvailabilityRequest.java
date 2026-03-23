package com.gymplatform.modules.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CreateAvailabilityRequest {
    @NotNull(message = "Staff ID is required")
    private UUID staffId;
    @NotNull(message = "Facility ID is required")
    private UUID facilityId;
    private int dayOfWeek;
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    private boolean recurring;
    private LocalDate specificDate;
    private boolean available = true;
}
