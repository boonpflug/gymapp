package com.gymplatform.modules.appointment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SubmitAnamneseRequest {
    @NotNull(message = "Form ID is required")
    private UUID formId;
    @NotNull(message = "Member ID is required")
    private UUID memberId;
    private UUID appointmentId;
    private String notes;
    @NotEmpty(message = "At least one answer is required")
    private List<AnswerRequest> answers;
}
