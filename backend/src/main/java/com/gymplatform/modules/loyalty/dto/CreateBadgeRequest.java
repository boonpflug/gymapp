package com.gymplatform.modules.loyalty.dto;

import com.gymplatform.modules.loyalty.BadgeCategory;
import com.gymplatform.modules.loyalty.BadgeCriteriaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBadgeRequest {
    @NotBlank(message = "Badge name is required")
    private String name;
    private String description;
    private String icon;
    @NotNull(message = "Badge category is required")
    private BadgeCategory category;
    @NotNull(message = "Criteria type is required")
    private BadgeCriteriaType criteriaType;
    @Min(1)
    private Integer criteriaValue;
}
