package com.gymplatform.modules.training.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderExercisesRequest {
    private List<UUID> exerciseIds;
}
