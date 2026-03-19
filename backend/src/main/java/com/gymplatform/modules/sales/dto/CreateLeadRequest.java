package com.gymplatform.modules.sales.dto;

import com.gymplatform.modules.sales.LeadSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateLeadRequest {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    private String email;
    private String phone;
    @NotNull(message = "Source is required")
    private LeadSource source;
    private String interest;
    private UUID assignedStaffId;
    private String notes;
    private UUID referralMemberId;
}
