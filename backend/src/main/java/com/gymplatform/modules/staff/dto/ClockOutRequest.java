package com.gymplatform.modules.staff.dto;

import lombok.Data;

@Data
public class ClockOutRequest {
    private Integer breakMinutes;
    private String notes;
}
