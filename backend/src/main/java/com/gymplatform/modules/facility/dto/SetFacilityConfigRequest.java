package com.gymplatform.modules.facility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SetFacilityConfigRequest {
    @NotBlank
    private String configKey;
    private String configValue;
    private String description;
}
