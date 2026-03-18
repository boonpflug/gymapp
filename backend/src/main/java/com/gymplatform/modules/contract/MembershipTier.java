package com.gymplatform.modules.contract;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "membership_tiers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MembershipTier extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Column(name = "minimum_term_months")
    private int minimumTermMonths;

    @Column(name = "notice_period_days")
    private int noticePeriodDays = 30;

    @Column(name = "class_allowance")
    private Integer classAllowance;

    @Column(name = "access_rules", columnDefinition = "TEXT")
    private String accessRules;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "tenant_id")
    private String tenantId;
}
