package com.gymplatform.modules.machine;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_sensor_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MachineSensorSession extends BaseEntity {

    @Column(name = "machine_id", nullable = false)
    private UUID machineId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "training_session_id")
    private UUID trainingSessionId;

    @Column(name = "training_log_id")
    private UUID trainingLogId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "sensor_data", columnDefinition = "TEXT")
    private String sensorData;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
