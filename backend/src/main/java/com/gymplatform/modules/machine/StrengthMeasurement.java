package com.gymplatform.modules.machine;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "strength_measurements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StrengthMeasurement extends BaseEntity {

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "sensor_session_id")
    private UUID sensorSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", nullable = false, length = 30)
    private MeasurementType measurementType;

    @Column(name = "peak_force_newtons", precision = 10, scale = 2)
    private BigDecimal peakForceNewtons;

    @Column(name = "avg_force_newtons", precision = 10, scale = 2)
    private BigDecimal avgForceNewtons;

    @Column(name = "range_of_motion_degrees", precision = 6, scale = 2)
    private BigDecimal rangeOfMotionDegrees;

    @Column(name = "time_under_tension_seconds", precision = 8, scale = 2)
    private BigDecimal timeUnderTensionSeconds;

    private Integer repetitions;

    @Column(name = "set_number")
    private Integer setNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
