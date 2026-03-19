package com.gymplatform.modules.booking.dto;

import com.gymplatform.modules.booking.RecurrenceRule;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ClassScheduleDto {
    private UUID id;
    private UUID classId;
    private String className;
    private String categoryName;
    private String categoryColor;
    private UUID trainerId;
    private String trainerName;
    private Instant startTime;
    private Instant endTime;
    private String room;
    private int capacity;
    private long bookedCount;
    private long waitlistCount;
    private String virtualLink;
    private boolean cancelled;
    private String cancellationReason;
    private RecurrenceRule recurrenceRule;
    private UUID recurrenceGroupId;
    private Instant createdAt;
}
