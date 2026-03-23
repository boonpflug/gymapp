package com.gymplatform.modules.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RevenueReportDto {
    private List<MonthlyRevenueDto> monthlyRevenue;
    private BigDecimal totalRevenue;
    private BigDecimal mrr;
    private BigDecimal outstandingReceivables;
    private double paymentSuccessRate;
}
