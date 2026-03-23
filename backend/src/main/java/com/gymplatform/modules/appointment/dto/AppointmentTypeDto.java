package com.gymplatform.modules.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AppointmentTypeDto {
    private UUID id;
    private String name;
    private String description;
    private int durationMinutes;
    private String color;
    private boolean requiresTrainer;
    private boolean active;
    private Instant createdAt;
}
