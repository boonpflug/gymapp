package com.gymplatform.modules.facility;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "facility_configurations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FacilityConfiguration extends BaseEntity {

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "text")
    private String configValue;

    @Column(length = 500)
    private String description;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
