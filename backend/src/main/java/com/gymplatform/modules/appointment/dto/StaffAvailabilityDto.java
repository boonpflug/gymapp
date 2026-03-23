package com.gymplatform.modules.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class StaffAvailabilityDto {
    private UUID id;
    private UUID staffId;
    private UUID facilityId;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean recurring;
    private LocalDate specificDate;
    private boolean available;
    private Instant createdAt;
}
