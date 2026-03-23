package com.gymplatform.modules.reporting;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.reporting.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final JdbcTemplate jdbcTemplate;

    private String schema() {
        String t = TenantContext.getTenantId();
        return (t != null && !t.equals("public")) ? t + "." : "";
    }

    // ── Revenue Report ──────────────────────────────────────

    public RevenueReportDto getRevenueReport(LocalDate from, LocalDate to) {
        String s = schema();
        Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Timestamp fromTs = Timestamp.from(fromInstant);
        Timestamp toTs = Timestamp.from(toInstant);

        // Monthly revenue breakdown
        List<MonthlyRevenueDto> monthly = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT TO_CHAR(issued_at, 'YYYY-MM') AS month, " +
                    "COALESCE(SUM(total_amount), 0) AS revenue, " +
                    "COUNT(*) AS invoice_count, " +
                    "COUNT(*) FILTER (WHERE status = 'PAID') AS paid_count " +
                    "FROM " + s + "invoices " +
                    "WHERE issued_at >= ? AND issued_at < ? " +
                    "GROUP BY month ORDER BY month",
                    fromTs, toTs);
            for (Map<String, Object> row : rows) {
                monthly.add(MonthlyRevenueDto.builder()
                        .month((String) row.get("month"))
                        .revenue(toBigDecimal(row.get("revenue")))
                        .invoiceCount(toInt(row.get("invoice_count")))
                        .paidCount(toInt(row.get("paid_count")))
                        .build());
            }
        } catch (Exception ignored) {}

        // Total revenue (paid invoices in period)
        BigDecimal totalRevenue = BigDecimal.ZERO;
        try {
            totalRevenue = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(total_amount), 0) FROM " + s + "invoices " +
                    "WHERE status = 'PAID' AND issued_at >= ? AND issued_at < ?",
                    BigDecimal.class, fromTs, toTs), BigDecimal.ZERO);
        } catch (Exception ignored) {}

        // MRR — sum of active contract monthly amounts
        BigDecimal mrr = BigDecimal.ZERO;
        try {
            mrr = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(monthly_amount), 0) FROM " + s + "contracts WHERE status = 'ACTIVE'",
                    BigDecimal.class), BigDecimal.ZERO);
        } catch (Exception ignored) {}

        // Outstanding receivables
        BigDecimal outstanding = BigDecimal.ZERO;
        try {
            outstanding = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(total_amount), 0) FROM " + s + "invoices WHERE status IN ('ISSUED', 'OVERDUE')",
                    BigDecimal.class), BigDecimal.ZERO);
        } catch (Exception ignored) {}

        // Payment success rate
        double successRate = 0.0;
        try {
            Map<String, Object> rates = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) AS total, " +
                    "COUNT(*) FILTER (WHERE status = 'SUCCESS') AS success " +
                    "FROM " + s + "payments WHERE processed_at >= ? AND processed_at < ?",
                    fromTs, toTs);
            long total = toLong(rates.get("total"));
            long success = toLong(rates.get("success"));
            if (total > 0) successRate = (double) success / total * 100.0;
        } catch (Exception ignored) {}

        return RevenueReportDto.builder()
                .monthlyRevenue(monthly)
                .totalRevenue(totalRevenue)
                .mrr(mrr)
                .outstandingReceivables(outstanding)
                .paymentSuccessRate(Math.round(successRate * 100.0) / 100.0)
                .build();
    }

    // ── Member Report ───────────────────────────────────────

    public MemberReportDto getMemberReport(LocalDate from, LocalDate to) {
        String s = schema();

        long totalActive = 0;
        long totalInactive = 0;
        long newThisMonth = 0;
        try {
            totalActive = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + s + "members WHERE status = 'ACTIVE'", Long.class), 0L);
        } catch (Exception ignored) {}
        try {
            totalInactive = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + s + "members WHERE status = 'INACTIVE'", Long.class), 0L);
        } catch (Exception ignored) {}
        try {
            newThisMonth = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + s + "members WHERE join_date >= ? AND join_date <= ?",
                    Long.class, Date.valueOf(from), Date.valueOf(to)), 0L);
        } catch (Exception ignored) {}

        // Churn rate: cancellations in period / total at start
        double churnRate = 0.0;
        try {
            long cancellations = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + s + "contracts WHERE status IN ('CANCELLED', 'PENDING_CANCELLATION') " +
                    "AND cancellation_date >= ? AND cancellation_date <= ?",
                    Long.class, Date.valueOf(from), Date.valueOf(to)), 0L);
            long totalMembers = totalActive + totalInactive;
            if (totalMembers > 0) churnRate = (double) cancellations / totalMembers * 100.0;
        } catch (Exception ignored) {}

        // Cancellation reasons
        Map<String, Long> reasons = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT COALESCE(cancellation_reason, 'Not specified') AS reason, COUNT(*) AS cnt " +
                    "FROM " + s + "contracts " +
                    "WHERE status IN ('CANCELLED', 'PENDING_CANCELLATION') " +
                    "AND cancellation_date >= ? AND cancellation_date <= ? " +
                    "GROUP BY reason ORDER BY cnt DESC",
                    Date.valueOf(from), Date.valueOf(to));
            for (Map<String, Object> row : rows) {
                reasons.put((String) row.get("reason"), toLong(row.get("cnt")));
            }
        } catch (Exception ignored) {}

        return MemberReportDto.builder()
                .totalActive(totalActive)
                .totalInactive(totalInactive)
                .newThisMonth(newThisMonth)
                .churnRate(Math.round(churnRate * 100.0) / 100.0)
                .cancellationReasons(reasons)
                .build();
    }

    // ── Check-in Report ─────────────────────────────────────

    public CheckInReportDto getCheckInReport(LocalDate from, LocalDate to) {
        String s = schema();
        Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Timestamp fromTs = Timestamp.from(fromInstant);
        Timestamp toTs = Timestamp.from(toInstant);

        long totalCheckIns = 0;
        try {
            totalCheckIns = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + s + "check_ins WHERE status = 'SUCCESS' " +
                    "AND check_in_time >= ? AND check_in_time < ?",
                    Long.class, fromTs, toTs), 0L);
        } catch (Exception ignored) {}

        // Average daily
        long daysBetween = Math.max(1, ChronoUnit.DAYS.between(from, to) + 1);
        double avgDaily = (double) totalCheckIns / daysBetween;

        // Peak hours
        Map<Integer, Long> peakHours = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT EXTRACT(HOUR FROM check_in_time) AS hour, COUNT(*) AS cnt " +
                    "FROM " + s + "check_ins WHERE check_in_time >= ? AND check_in_time < ? " +
                    "AND status = 'SUCCESS' " +
                    "GROUP BY hour ORDER BY cnt DESC",
                    fromTs, toTs);
            for (Map<String, Object> row : rows) {
                peakHours.put(toInt(row.get("hour")), toLong(row.get("cnt")));
            }
        } catch (Exception ignored) {}

        // Peak days
        Map<String, Long> peakDays = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT TO_CHAR(check_in_time, 'Day') AS dow, COUNT(*) AS cnt " +
                    "FROM " + s + "check_ins WHERE check_in_time >= ? AND check_in_time < ? " +
                    "AND status = 'SUCCESS' " +
                    "GROUP BY dow ORDER BY cnt DESC",
                    fromTs, toTs);
            for (Map<String, Object> row : rows) {
                peakDays.put(((String) row.get("dow")).trim(), toLong(row.get("cnt")));
            }
        } catch (Exception ignored) {}

        // Top 10 members
        List<TopCheckinMemberDto> topMembers = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT ci.member_id, m.first_name || ' ' || m.last_name AS member_name, COUNT(*) AS cnt " +
                    "FROM " + s + "check_ins ci " +
                    "JOIN " + s + "members m ON ci.member_id = m.id " +
                    "WHERE ci.check_in_time >= ? AND ci.check_in_time < ? AND ci.status = 'SUCCESS' " +
                    "GROUP BY ci.member_id, member_name ORDER BY cnt DESC LIMIT 10",
                    fromTs, toTs);
            for (Map<String, Object> row : rows) {
                topMembers.add(TopCheckinMemberDto.builder()
                        .memberId((UUID) row.get("member_id"))
                        .memberName((String) row.get("member_name"))
                        .checkInCount(toLong(row.get("cnt")))
                        .build());
            }
        } catch (Exception ignored) {}

        return CheckInReportDto.builder()
                .totalCheckIns(totalCheckIns)
                .avgDaily(Math.round(avgDaily * 100.0) / 100.0)
                .peakHours(peakHours)
                .peakDays(peakDays)
                .topMembers(topMembers)
                .build();
    }

    // ── Class Report ────────────────────────────────────────

    public ClassReportDto getClassReport(LocalDate from, LocalDate to) {
        String s = schema();
        Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Timestamp fromTs = Timestamp.from(fromInstant);
        Timestamp toTs = Timestamp.from(toInstant);

        long totalClasses = 0;
        try {
            totalClasses = Objects.requireNonNullElse(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + s + "class_schedules " +
                    "WHERE start_time >= ? AND start_time < ? AND cancelled = false",
                    Long.class, fromTs, toTs), 0L);
        } catch (Exception ignored) {}

        // Class popularity with booking stats
        List<ClassPopularityDto> allClasses = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT c.name AS class_name, " +
                    "COUNT(DISTINCT cs.id) AS schedule_count, " +
                    "COUNT(cb.id) AS total_bookings, " +
                    "COUNT(cb.id) FILTER (WHERE cb.status = 'ATTENDED') AS attended, " +
                    "AVG(cs.capacity) AS avg_capacity " +
                    "FROM " + s + "class_schedules cs " +
                    "JOIN " + s + "classes c ON cs.class_id = c.id " +
                    "LEFT JOIN " + s + "class_bookings cb ON cb.schedule_id = cs.id " +
                    "WHERE cs.start_time >= ? AND cs.start_time < ? AND cs.cancelled = false " +
                    "GROUP BY c.name ORDER BY total_bookings DESC",
                    fromTs, toTs);
            for (Map<String, Object> row : rows) {
                long scheduleCount = toLong(row.get("schedule_count"));
                long totalBookings = toLong(row.get("total_bookings"));
                long attended = toLong(row.get("attended"));
                double avgCapacity = toDouble(row.get("avg_capacity"));

                double avgAttendance = scheduleCount > 0 ? (double) attended / scheduleCount : 0;
                double utilization = (avgCapacity > 0 && scheduleCount > 0)
                        ? ((double) totalBookings / scheduleCount) / avgCapacity * 100.0 : 0;

                allClasses.add(ClassPopularityDto.builder()
                        .className((String) row.get("class_name"))
                        .totalBookings(totalBookings)
                        .avgAttendance(Math.round(avgAttendance * 100.0) / 100.0)
                        .capacityUtilization(Math.round(utilization * 100.0) / 100.0)
                        .build());
            }
        } catch (Exception ignored) {}

        // Average attendance rate across all classes
        double avgAttendanceRate = 0.0;
        if (!allClasses.isEmpty()) {
            avgAttendanceRate = allClasses.stream()
                    .mapToDouble(ClassPopularityDto::getCapacityUtilization)
                    .average().orElse(0.0);
        }

        // Most and least popular (top/bottom 5)
        List<ClassPopularityDto> mostPopular = allClasses.stream().limit(5).collect(Collectors.toList());
        List<ClassPopularityDto> leastPopular = allClasses.size() > 5
                ? allClasses.subList(Math.max(0, allClasses.size() - 5), allClasses.size())
                        .stream().collect(Collectors.toList())
                : Collections.emptyList();
        Collections.reverse(leastPopular);

        return ClassReportDto.builder()
                .totalClasses(totalClasses)
                .avgAttendanceRate(Math.round(avgAttendanceRate * 100.0) / 100.0)
                .mostPopular(mostPopular)
                .leastPopular(leastPopular)
                .build();
    }

    // ── CSV Export ───────────────────────────────────────────

    public String exportToCsv(String reportType, LocalDate from, LocalDate to) {
        return switch (reportType.toUpperCase()) {
            case "REVENUE" -> exportRevenueCsv(from, to);
            case "MEMBERS" -> exportMembersCsv(from, to);
            case "CHECKINS" -> exportCheckinsCsv(from, to);
            case "CLASSES" -> exportClassesCsv(from, to);
            default -> throw new IllegalArgumentException("Unknown report type: " + reportType);
        };
    }

    private String exportRevenueCsv(LocalDate from, LocalDate to) {
        RevenueReportDto report = getRevenueReport(from, to);
        StringBuilder sb = new StringBuilder();
        sb.append("Month,Revenue,Invoice Count,Paid Count\n");
        for (MonthlyRevenueDto m : report.getMonthlyRevenue()) {
            sb.append(csvEscape(m.getMonth())).append(",")
              .append(m.getRevenue()).append(",")
              .append(m.getInvoiceCount()).append(",")
              .append(m.getPaidCount()).append("\n");
        }
        sb.append("\nSummary\n");
        sb.append("Total Revenue,").append(report.getTotalRevenue()).append("\n");
        sb.append("MRR,").append(report.getMrr()).append("\n");
        sb.append("Outstanding Receivables,").append(report.getOutstandingReceivables()).append("\n");
        sb.append("Payment Success Rate,").append(report.getPaymentSuccessRate()).append("%\n");
        return sb.toString();
    }

    private String exportMembersCsv(LocalDate from, LocalDate to) {
        MemberReportDto report = getMemberReport(from, to);
        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("Total Active,").append(report.getTotalActive()).append("\n");
        sb.append("Total Inactive,").append(report.getTotalInactive()).append("\n");
        sb.append("New This Period,").append(report.getNewThisMonth()).append("\n");
        sb.append("Churn Rate,").append(report.getChurnRate()).append("%\n");
        sb.append("\nCancellation Reason,Count\n");
        for (Map.Entry<String, Long> entry : report.getCancellationReasons().entrySet()) {
            sb.append(csvEscape(entry.getKey())).append(",").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String exportCheckinsCsv(LocalDate from, LocalDate to) {
        CheckInReportDto report = getCheckInReport(from, to);
        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("Total Check-ins,").append(report.getTotalCheckIns()).append("\n");
        sb.append("Avg Daily,").append(report.getAvgDaily()).append("\n");
        sb.append("\nHour,Count\n");
        for (Map.Entry<Integer, Long> entry : report.getPeakHours().entrySet()) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
        }
        sb.append("\nDay,Count\n");
        for (Map.Entry<String, Long> entry : report.getPeakDays().entrySet()) {
            sb.append(csvEscape(entry.getKey())).append(",").append(entry.getValue()).append("\n");
        }
        sb.append("\nTop Members\nMember ID,Member Name,Check-in Count\n");
        for (TopCheckinMemberDto m : report.getTopMembers()) {
            sb.append(m.getMemberId()).append(",")
              .append(csvEscape(m.getMemberName())).append(",")
              .append(m.getCheckInCount()).append("\n");
        }
        return sb.toString();
    }

    private String exportClassesCsv(LocalDate from, LocalDate to) {
        ClassReportDto report = getClassReport(from, to);
        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("Total Classes,").append(report.getTotalClasses()).append("\n");
        sb.append("Avg Attendance Rate,").append(report.getAvgAttendanceRate()).append("%\n");
        sb.append("\nMost Popular Classes\nClass Name,Total Bookings,Avg Attendance,Capacity Utilization %\n");
        for (ClassPopularityDto c : report.getMostPopular()) {
            sb.append(csvEscape(c.getClassName())).append(",")
              .append(c.getTotalBookings()).append(",")
              .append(c.getAvgAttendance()).append(",")
              .append(c.getCapacityUtilization()).append("\n");
        }
        sb.append("\nLeast Popular Classes\nClass Name,Total Bookings,Avg Attendance,Capacity Utilization %\n");
        for (ClassPopularityDto c : report.getLeastPopular()) {
            sb.append(csvEscape(c.getClassName())).append(",")
              .append(c.getTotalBookings()).append(",")
              .append(c.getAvgAttendance()).append(",")
              .append(c.getCapacityUtilization()).append("\n");
        }
        return sb.toString();
    }

    // ── Helpers ─────────────────────────────────────────────

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }

    private long toLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }
}
