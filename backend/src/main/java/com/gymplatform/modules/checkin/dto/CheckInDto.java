package com.gymplatform.modules.checkin.dto;

import com.gymplatform.modules.checkin.CheckInMethod;
import com.gymplatform.modules.checkin.CheckInStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CheckInDto {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private String memberNumber;
    private UUID deviceId;
    private String deviceName;
    private CheckInMethod method;
    private CheckInStatus status;
    private String denialReason;
    private UUID staffId;
    private Instant checkInTime;
    private Instant checkOutTime;
}
