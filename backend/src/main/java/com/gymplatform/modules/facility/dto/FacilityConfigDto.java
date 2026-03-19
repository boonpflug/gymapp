package com.gymplatform.modules.facility.dto;

import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FacilityConfigDto {
    private UUID id;
    private UUID facilityId;
    private String configKey;
    private String configValue;
    private String description;
}
