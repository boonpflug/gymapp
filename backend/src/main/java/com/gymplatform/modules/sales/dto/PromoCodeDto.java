package com.gymplatform.modules.sales.dto;

import com.gymplatform.modules.sales.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PromoCodeDto {
    private UUID id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Instant expiresAt;
    private Integer maxUsages;
    private int currentUsages;
    private boolean active;
    private boolean expired;
    private boolean exhausted;
    private Instant createdAt;
}
