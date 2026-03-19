package com.gymplatform.modules.training.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateGoalProgressRequest {
    private BigDecimal currentValue;
}
