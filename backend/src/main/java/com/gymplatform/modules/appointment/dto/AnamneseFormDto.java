package com.gymplatform.modules.appointment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AnamneseFormDto {
    private UUID id;
    private String name;
    private String description;
    private int version;
    private boolean active;
    private List<AnamneseQuestionDto> questions;
    private Instant createdAt;
}
