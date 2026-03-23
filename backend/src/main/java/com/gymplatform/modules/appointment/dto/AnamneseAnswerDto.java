package com.gymplatform.modules.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AnamneseAnswerDto {
    private UUID id;
    private UUID questionId;
    private String questionText;
    private String answerText;
    private BigDecimal answerNumber;
    private Boolean answerBoolean;
}
