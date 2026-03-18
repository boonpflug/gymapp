package com.gymplatform.modules.contract;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "idle_periods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdlePeriod extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "billing_reduced")
    private boolean billingReduced;

    @Column(name = "reduced_amount", precision = 10, scale = 2)
    private BigDecimal reducedAmount;
}
