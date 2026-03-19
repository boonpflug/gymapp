package com.gymplatform.modules.facility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssignMemberFacilityRequest {
    @NotNull
    private UUID memberId;
    @NotNull
    private UUID facilityId;
    private boolean homeFacility;
    private boolean crossFacilityAccess = true;
}
