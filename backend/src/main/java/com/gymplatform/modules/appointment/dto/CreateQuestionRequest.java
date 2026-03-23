package com.gymplatform.modules.appointment.dto;

import com.gymplatform.modules.appointment.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateQuestionRequest {
    @NotBlank(message = "Question text is required")
    private String questionText;
    @NotNull(message = "Question type is required")
    private QuestionType questionType;
    private String options;
    private boolean required;
    private int sortOrder;
    private String section;
}
