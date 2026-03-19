package com.gymplatform.modules.training.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TrainingSessionDto {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private UUID planId;
    private String planName;
    private Instant startedAt;
    private Instant finishedAt;
    private Integer durationMinutes;
    private String notes;
    private Integer rating;
    private List<TrainingLogDto> logs;
    private Instant createdAt;
}
