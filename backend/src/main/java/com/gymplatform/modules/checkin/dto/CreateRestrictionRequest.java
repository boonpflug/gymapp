package com.gymplatform.modules.checkin.dto;

import com.gymplatform.modules.checkin.RestrictionReason;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateRestrictionRequest {

    @NotNull(message = "Member ID is required")
    private UUID memberId;

    @NotNull(message = "Reason is required")
    private RestrictionReason reason;

    private String description;
    private boolean autoRelease;
}
