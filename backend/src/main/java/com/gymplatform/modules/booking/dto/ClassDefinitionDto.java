package com.gymplatform.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ClassDefinitionDto {
    private UUID id;
    private String name;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID trainerId;
    private String trainerName;
    private String room;
    private int capacity;
    private int durationMinutes;
    private String virtualLink;
    private boolean allowWaitlist;
    private int bookingCutoffMinutes;
    private int cancellationCutoffMinutes;
    private boolean allowTrial;
    private boolean active;
    private Instant createdAt;
}
