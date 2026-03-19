package com.gymplatform.modules.facility.dto;

import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MemberFacilityAccessDto {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private UUID facilityId;
    private String facilityName;
    private boolean homeFacility;
    private boolean crossFacilityAccess;
}
