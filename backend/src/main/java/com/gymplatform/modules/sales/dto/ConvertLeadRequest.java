package com.gymplatform.modules.sales.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ConvertLeadRequest {
    private UUID membershipTierId;
    private String promoCode;
}
