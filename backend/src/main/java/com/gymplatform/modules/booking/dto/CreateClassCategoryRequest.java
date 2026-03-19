package com.gymplatform.modules.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateClassCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
    private String description;
    private String color;
}
