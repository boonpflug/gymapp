package com.gymplatform.modules.contract.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateContractRequest {
    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotNull(message = "Membership tier ID is required")
    private UUID membershipTierId;

    private LocalDate startDate;
    private String discountCode;
}
