package com.gymplatform.modules.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class AnamneseSubmissionDto {
    private UUID id;
    private UUID formId;
    private String formName;
    private UUID memberId;
    private String memberName;
    private UUID appointmentId;
    private UUID submittedBy;
    private Instant submittedAt;
    private String notes;
    private List<AnamneseAnswerDto> answers;
    private Instant createdAt;
}
