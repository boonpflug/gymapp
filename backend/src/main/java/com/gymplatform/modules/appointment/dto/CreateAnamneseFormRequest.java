package com.gymplatform.modules.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateAnamneseFormRequest {
    @NotBlank(message = "Form name is required")
    private String name;
    private String description;
    @NotEmpty(message = "At least one question is required")
    private List<CreateQuestionRequest> questions;
}
