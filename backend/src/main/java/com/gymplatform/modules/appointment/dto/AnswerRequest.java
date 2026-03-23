package com.gymplatform.modules.appointment.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AnswerRequest {
    @NotNull(message = "Question ID is required")
    private UUID questionId;
    private String answerText;
    private BigDecimal answerNumber;
    private Boolean answerBoolean;
}
