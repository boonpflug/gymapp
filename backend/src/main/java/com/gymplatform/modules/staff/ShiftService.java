package com.gymplatform.modules.staff;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.staff.dto.CreateShiftRequest;
import com.gymplatform.modules.staff.dto.ShiftDto;
import com.gymplatform.modules.staff.dto.ShiftReportDto;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final EmployeeService employeeService;
    private final TimeEntryRepository timeEntryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ShiftDto create(CreateShiftRequest req, UUID userId) {
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw BusinessException.badRequest("End time must be after start time");
        }

        Shift shift = Shift.builder()
                .employeeId(req.getEmployeeId())
                .facilityId(req.getFacilityId())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(ShiftStatus.SCHEDULED)
                .notes(req.getNotes())
                .tenantId(TenantContext.getTenantId())
                .build();
        shift = shiftRepository.save(shift);
        auditLogService.log("Shift", shift.getId(), "CREATE", userId, null, null);
        return toDto(shift);
    }

    @Transactional
    public ShiftDto updateStatus(UUID shiftId, ShiftStatus status, UUID userId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> BusinessException.notFound("Shift", shiftId));
        shift.setStatus(status);
        shift = shiftRepository.save(shift);
        auditLogService.log("Shift", shiftId, "STATUS_" + status, userId, null, null);
        return toDto(shift);
    }

    @Transactional
    public void cancel(UUID shiftId, UUID userId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> BusinessException.notFound("Shift", shiftId));
        shift.setStatus(ShiftStatus.CANCELLED);
        shiftRepository.save(shift);
        auditLogService.log("Shift", shiftId, "CANCEL", userId, null, null);
    }

    public List<ShiftDto> getByEmployee(UUID employeeId, Instant start, Instant end) {
        return shiftRepository.findByEmployeeIdAndStartTimeBetweenOrderByStartTimeAsc(employeeId, start, end)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ShiftDto> getWeeklySchedule(Instant weekStart) {
        Instant weekEnd = weekStart.plus(7, ChronoUnit.DAYS);
        return shiftRepository.findByStartTimeBetweenOrderByStartTimeAsc(weekStart, weekEnd)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ShiftDto> getUpcoming(UUID employeeId) {
        return shiftRepository.findUpcomingByEmployee(employeeId, Instant.now())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ShiftReportDto> getShiftVsActualReport(Instant start, Instant end) {
        List<Shift> shifts = shiftRepository.findByStartTimeBetweenOrderByStartTimeAsc(start, end);

        var grouped = shifts.stream()
                .collect(Collectors.groupingBy(Shift::getEmployeeId));

        return grouped.entrySet().stream().map(entry -> {
            UUID empId = entry.getKey();
            List<Shift> empShifts = entry.getValue();

            int scheduledMinutes = empShifts.stream()
                    .mapToInt(s -> (int) Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                    .sum();
            int actualMinutes = timeEntryRepository.sumMinutesByEmployeeBetween(empId, start, end);

            return ShiftReportDto.builder()
                    .employeeId(empId)
                    .employeeName(employeeService.getEmployeeName(empId))
                    .scheduledMinutes(scheduledMinutes)
                    .actualMinutes(actualMinutes)
                    .difference(actualMinutes - scheduledMinutes)
                    .shiftCount(empShifts.size())
                    .timeEntryCount(
                            timeEntryRepository.findByEmployeeIdAndClockInBetweenOrderByClockInAsc(empId, start, end).size()
                    )
                    .build();
        }).collect(Collectors.toList());
    }

    private ShiftDto toDto(Shift s) {
        long duration = Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
        return ShiftDto.builder()
                .id(s.getId())
                .employeeId(s.getEmployeeId())
                .employeeName(employeeService.getEmployeeName(s.getEmployeeId()))
                .facilityId(s.getFacilityId())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .notes(s.getNotes())
                .durationMinutes(duration)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
