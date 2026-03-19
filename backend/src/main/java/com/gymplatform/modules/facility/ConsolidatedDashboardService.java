package com.gymplatform.modules.facility;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.facility.dto.ConsolidatedDashboardDto;
import com.gymplatform.modules.facility.dto.ConsolidatedDashboardDto.FacilitySummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsolidatedDashboardService {

    private final FacilityRepository facilityRepository;
    private final FacilityService facilityService;
    private final MemberFacilityAccessRepository memberAccessRepository;
    private final JdbcTemplate jdbcTemplate;

    private String schema() {
        String t = TenantContext.getTenantId();
        return (t != null && !t.equals("public")) ? t + "." : "";
    }

    public ConsolidatedDashboardDto getDashboard() {
        List<Facility> facilities = facilityRepository.findByActiveTrueOrderByNameAsc();

        long totalActiveMembers = countTotalActiveMembers();
        long totalCheckInsToday = countCheckInsToday();
        long totalNewMembersThisMonth = countNewMembersThisMonth();
        BigDecimal totalRevenue = getTotalRevenueThisMonth();
        BigDecimal totalOutstanding = getTotalOutstandingPayments();

        List<FacilitySummaryDto> summaries = new ArrayList<>();
        for (Facility facility : facilities) {
            summaries.add(buildFacilitySummary(facility));
        }

        return ConsolidatedDashboardDto.builder()
                .totalFacilities(facilities.size())
                .totalActiveMembers(totalActiveMembers)
                .totalCheckInsToday(totalCheckInsToday)
                .totalNewMembersThisMonth(totalNewMembersThisMonth)
                .totalRevenueThisMonth(totalRevenue)
                .totalOutstandingPayments(totalOutstanding)
                .facilitySummaries(summaries)
                .build();
    }

    private FacilitySummaryDto buildFacilitySummary(Facility facility) {
        UUID facilityId = facility.getId();

        long activeMembers = memberAccessRepository.countByFacilityId(facilityId);

        long checkInsToday = countCheckInsTodayForFacility(facilityId);

        long newMembers = countNewMembersThisMonthForFacility(facilityId);

        BigDecimal revenue = getRevenueThisMonthForFacility(facilityId);

        int occupancy = getCurrentOccupancyForFacility(facilityId);

        return FacilitySummaryDto.builder()
                .facility(facilityService.toDto(facility))
                .activeMembers(activeMembers)
                .checkInsToday(checkInsToday)
                .newMembersThisMonth(newMembers)
                .revenueThisMonth(revenue != null ? revenue : BigDecimal.ZERO)
                .currentOccupancy(occupancy)
                .build();
    }

    private long countTotalActiveMembers() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema() + "members WHERE status = 'ACTIVE'", Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private long countCheckInsToday() {
        try {
            Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema() + "check_ins WHERE check_in_time >= ? AND status = 'SUCCESS'",
                    Long.class, java.sql.Timestamp.from(startOfDay));
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private long countNewMembersThisMonth() {
        try {
            LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema() + "members WHERE join_date >= ?",
                    Long.class, java.sql.Date.valueOf(firstOfMonth));
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal getTotalRevenueThisMonth() {
        try {
            Instant firstOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            BigDecimal revenue = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM " + schema() + "payments WHERE status = 'SUCCESS' AND processed_at >= ?",
                    BigDecimal.class, java.sql.Timestamp.from(firstOfMonth));
            return revenue != null ? revenue : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getTotalOutstandingPayments() {
        try {
            BigDecimal outstanding = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(total_amount), 0) FROM " + schema() + "invoices WHERE status = 'OVERDUE'",
                    BigDecimal.class);
            return outstanding != null ? outstanding : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long countCheckInsTodayForFacility(UUID facilityId) {
        try {
            Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema() + "check_ins ci " +
                    "JOIN " + schema() + "member_facility_access mfa ON ci.member_id = mfa.member_id AND mfa.facility_id = ? " +
                    "WHERE ci.check_in_time >= ? AND ci.status = 'SUCCESS'",
                    Long.class, facilityId, java.sql.Timestamp.from(startOfDay));
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private long countNewMembersThisMonthForFacility(UUID facilityId) {
        try {
            LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema() + "members m " +
                    "JOIN " + schema() + "member_facility_access mfa ON m.id = mfa.member_id AND mfa.facility_id = ? " +
                    "WHERE m.join_date >= ?",
                    Long.class, facilityId, java.sql.Date.valueOf(firstOfMonth));
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal getRevenueThisMonthForFacility(UUID facilityId) {
        try {
            Instant firstOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            BigDecimal revenue = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(p.amount), 0) FROM " + schema() + "payments p " +
                    "JOIN " + schema() + "invoices i ON p.invoice_id = i.id " +
                    "JOIN " + schema() + "member_facility_access mfa ON i.member_id = mfa.member_id AND mfa.facility_id = ? " +
                    "WHERE p.status = 'SUCCESS' AND p.processed_at >= ?",
                    BigDecimal.class, facilityId, java.sql.Timestamp.from(firstOfMonth));
            return revenue != null ? revenue : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int getCurrentOccupancyForFacility(UUID facilityId) {
        try {
            Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + schema() + "check_ins ci " +
                    "JOIN " + schema() + "member_facility_access mfa ON ci.member_id = mfa.member_id AND mfa.facility_id = ? " +
                    "WHERE ci.check_in_time >= ? AND ci.check_out_time IS NULL AND ci.status = 'SUCCESS'",
                    Long.class, facilityId, java.sql.Timestamp.from(startOfDay));
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
