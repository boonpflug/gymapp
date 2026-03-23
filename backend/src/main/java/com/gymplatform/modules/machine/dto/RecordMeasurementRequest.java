package com.gymplatform.modules.machine.dto;

import com.gymplatform.modules.machine.MeasurementType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class RecordMeasurementRequest {
    @NotNull
    private UUID machineId;
    @NotNull
    private UUID memberId;
    private UUID sensorSessionId;
    @NotNull
    private MeasurementType measurementType;
    private BigDecimal peakForceNewtons;
    private BigDecimal avgForceNewtons;
    private BigDecimal rangeOfMotionDegrees;
    private BigDecimal timeUnderTensionSeconds;
    private Integer repetitions;
    private Integer setNumber;
    private String notes;
    @NotNull
    private Instant measuredAt;
}
