package com.gymplatform.modules.appointment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAppointmentTypeRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes;
    private String color;
    private boolean requiresTrainer;
}
