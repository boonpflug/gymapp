package com.gymplatform.modules.machine.dto;

import com.gymplatform.modules.machine.MeasurementType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class StrengthMeasurementDto {
    private UUID id;
    private UUID machineId;
    private String machineCode;
    private String machineName;
    private UUID memberId;
    private String memberName;
    private UUID sensorSessionId;
    private MeasurementType measurementType;
    private BigDecimal peakForceNewtons;
    private BigDecimal avgForceNewtons;
    private BigDecimal rangeOfMotionDegrees;
    private BigDecimal timeUnderTensionSeconds;
    private Integer repetitions;
    private Integer setNumber;
    private String notes;
    private Instant measuredAt;
    private Instant createdAt;
}
