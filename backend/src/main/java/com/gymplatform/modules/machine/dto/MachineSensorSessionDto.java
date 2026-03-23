package com.gymplatform.modules.machine.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MachineSensorSessionDto {
    private UUID id;
    private UUID machineId;
    private String machineCode;
    private String machineName;
    private UUID memberId;
    private String memberName;
    private UUID trainingSessionId;
    private UUID trainingLogId;
    private Instant startedAt;
    private Instant endedAt;
    private Integer durationSeconds;
    private String sensorData;
    private Instant createdAt;
}
