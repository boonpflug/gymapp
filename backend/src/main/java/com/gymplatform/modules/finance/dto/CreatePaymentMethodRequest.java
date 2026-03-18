package com.gymplatform.modules.finance.dto;

import com.gymplatform.modules.finance.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentMethodRequest {
    @NotNull(message = "Payment type is required")
    private PaymentType type;
    private String stripePaymentMethodId;
    private String goCardlessMandateId;
    private String last4;
    private boolean isDefault;
}
