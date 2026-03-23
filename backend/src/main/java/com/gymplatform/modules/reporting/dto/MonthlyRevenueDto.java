package com.gymplatform.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MonthlyRevenueDto {
    private String month;       // "2026-01"
    private BigDecimal revenue;
    private int invoiceCount;
    private int paidCount;
}
