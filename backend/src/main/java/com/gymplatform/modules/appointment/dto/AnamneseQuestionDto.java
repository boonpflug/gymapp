package com.gymplatform.modules.appointment.dto;

import com.gymplatform.modules.appointment.QuestionType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AnamneseQuestionDto {
    private UUID id;
    private String questionText;
    private QuestionType questionType;
    private String options;
    private boolean required;
    private int sortOrder;
    private String section;
}
