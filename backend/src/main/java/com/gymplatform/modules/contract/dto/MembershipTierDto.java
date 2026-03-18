package com.gymplatform.modules.contract.dto;

import com.gymplatform.modules.contract.BillingCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MembershipTierDto {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private BillingCycle billingCycle;
    private int minimumTermMonths;
    private int noticePeriodDays;
    private Integer classAllowance;
    private boolean active;
}
