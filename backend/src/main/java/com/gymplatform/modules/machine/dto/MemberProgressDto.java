package com.gymplatform.modules.machine.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MemberProgressDto {
    private UUID memberId;
    private String memberName;
    private UUID machineId;
    private String machineCode;
    private String machineName;
    private List<StrengthMeasurementDto> measurements;
    private BigDecimal initialPeakForce;
    private BigDecimal latestPeakForce;
    private double improvementPercent;
}
