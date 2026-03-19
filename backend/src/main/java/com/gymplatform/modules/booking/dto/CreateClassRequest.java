package com.gymplatform.modules.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateClassRequest {
    @NotBlank(message = "Class name is required")
    private String name;
    private String description;
    private UUID categoryId;
    private UUID trainerId;
    private String room;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private String virtualLink;
    private boolean allowWaitlist = true;
    private int bookingCutoffMinutes = 60;
    private int cancellationCutoffMinutes = 120;
    private boolean allowTrial = false;
}
