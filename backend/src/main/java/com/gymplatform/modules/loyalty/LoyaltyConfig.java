package com.gymplatform.modules.loyalty;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyConfig extends BaseEntity {

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
