package com.gymplatform.modules.loyalty.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateReferralRequest {
    @NotNull(message = "Referrer member ID is required")
    private UUID referrerMemberId;
    @NotBlank(message = "Referred email is required")
    @Email(message = "Must be a valid email address")
    private String referredEmail;
}
