package com.gymplatform.modules.finance.dto;

import com.gymplatform.modules.finance.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentMethodDto {
    private UUID id;
    private UUID memberId;
    private PaymentType type;
    private String last4;
    private boolean isDefault;
    private boolean active;
}
