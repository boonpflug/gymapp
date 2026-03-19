package com.gymplatform.modules.booking.dto;

import com.gymplatform.modules.booking.RecurrenceRule;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateScheduleRequest {
    @NotNull(message = "Class ID is required")
    private UUID classId;

    private UUID trainerId;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    private String room;
    private Integer capacityOverride;
    private String virtualLink;
    private RecurrenceRule recurrenceRule;
    private int recurrenceWeeks = 1;
}
