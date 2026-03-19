package com.gymplatform.modules.facility.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConsolidatedDashboardDto {
    private long totalFacilities;
    private long totalActiveMembers;
    private long totalCheckInsToday;
    private long totalNewMembersThisMonth;
    private BigDecimal totalRevenueThisMonth;
    private BigDecimal totalOutstandingPayments;
    private List<FacilitySummaryDto> facilitySummaries;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FacilitySummaryDto {
        private FacilityDto facility;
        private long activeMembers;
        private long checkInsToday;
        private long newMembersThisMonth;
        private BigDecimal revenueThisMonth;
        private int currentOccupancy;
    }
}
