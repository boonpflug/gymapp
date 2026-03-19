package com.gymplatform.modules.booking.dto;

import com.gymplatform.modules.booking.WaitlistStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class WaitlistEntryDto {
    private UUID id;
    private UUID scheduleId;
    private String className;
    private Instant classStartTime;
    private UUID memberId;
    private String memberName;
    private int position;
    private WaitlistStatus status;
    private Instant joinedAt;
    private Instant promotedAt;
}
