package com.gymplatform.modules.sales;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promo_code_usages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromoCodeUsage extends BaseEntity {

    @Column(name = "promo_code_id", nullable = false)
    private UUID promoCodeId;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
