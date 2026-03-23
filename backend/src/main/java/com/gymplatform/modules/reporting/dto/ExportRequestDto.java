package com.gymplatform.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ExportRequestDto {
    private String reportType;  // REVENUE, MEMBERS, CHECKINS, CLASSES
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String format;      // CSV, JSON
}
