package com.gymplatform.modules.machine.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class RecordSensorSessionRequest {
    @NotNull
    private UUID machineId;
    @NotNull
    private UUID memberId;
    private UUID trainingSessionId;
    private UUID trainingLogId;
    @NotNull
    private Instant startedAt;
    private Instant endedAt;
    private Integer durationSeconds;
    private String sensorData;
}
