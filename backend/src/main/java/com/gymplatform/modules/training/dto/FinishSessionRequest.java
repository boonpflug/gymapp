package com.gymplatform.modules.training.dto;

import lombok.Data;

@Data
public class FinishSessionRequest {
    private String notes;
    private Integer rating;
}
