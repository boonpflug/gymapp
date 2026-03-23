package com.gymplatform.modules.appointment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateAppointmentRequest {
    @NotNull(message = "Member ID is required")
    private UUID memberId;
    private UUID staffId;
    @NotNull(message = "Facility ID is required")
    private UUID facilityId;
    @NotNull(message = "Appointment type ID is required")
    private UUID appointmentTypeId;
    @NotNull(message = "Start time is required")
    private Instant startTime;
    private String notes;
    private String recurringRule;
}
