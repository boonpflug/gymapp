package com.gymplatform.modules.checkin;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "access_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessDevice extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 50)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DeviceMode mode;

    @Column(name = "location_description")
    private String locationDescription;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "max_occupancy")
    private Integer maxOccupancy;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
