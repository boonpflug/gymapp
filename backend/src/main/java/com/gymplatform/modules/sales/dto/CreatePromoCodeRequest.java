package com.gymplatform.modules.sales.dto;

import com.gymplatform.modules.sales.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CreatePromoCodeRequest {
    @NotBlank(message = "Promo code is required")
    private String code;
    private String description;
    @NotNull(message = "Discount type is required")
    private DiscountType discountType;
    @NotNull(message = "Discount value is required")
    private BigDecimal discountValue;
    private Instant expiresAt;
    private Integer maxUsages;
}
