package com.gymplatform.modules.appointment.dto;

import com.gymplatform.modules.appointment.AppointmentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AppointmentDto {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private UUID staffId;
    private String staffName;
    private UUID facilityId;
    private UUID appointmentTypeId;
    private String appointmentTypeName;
    private Instant startTime;
    private Instant endTime;
    private AppointmentStatus status;
    private String notes;
    private String cancellationReason;
    private Instant cancelledAt;
    private String recurringRule;
    private Instant createdAt;
}
