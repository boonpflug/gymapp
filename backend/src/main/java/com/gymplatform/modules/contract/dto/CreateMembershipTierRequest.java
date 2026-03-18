package com.gymplatform.modules.contract.dto;

import com.gymplatform.modules.contract.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMembershipTierRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;

    @NotNull(message = "Monthly price is required")
    @Positive(message = "Monthly price must be positive")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    private int minimumTermMonths;
    private int noticePeriodDays = 30;
    private Integer classAllowance;
    private String accessRules;
}
