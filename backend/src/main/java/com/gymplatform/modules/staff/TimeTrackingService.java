package com.gymplatform.modules.staff;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.staff.dto.ClockInRequest;
import com.gymplatform.modules.staff.dto.ClockOutRequest;
import com.gymplatform.modules.staff.dto.TimeEntryDto;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeTrackingService {

    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeService employeeService;
    private final ShiftRepository shiftRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public TimeEntryDto clockIn(ClockInRequest req, UUID userId) {
        // Check if already clocked in
        timeEntryRepository.findByEmployeeIdAndClockOutIsNull(req.getEmployeeId())
                .ifPresent(existing -> {
                    throw BusinessException.conflict("Employee is already clocked in");
                });

        TimeEntry entry = TimeEntry.builder()
                .employeeId(req.getEmployeeId())
                .shiftId(req.getShiftId())
                .clockIn(Instant.now())
                .notes(req.getNotes())
                .tenantId(TenantContext.getTenantId())
                .build();
        entry = timeEntryRepository.save(entry);

        // Update shift status if linked
        if (req.getShiftId() != null) {
            shiftRepository.findById(req.getShiftId()).ifPresent(shift -> {
                shift.setStatus(ShiftStatus.IN_PROGRESS);
                shiftRepository.save(shift);
            });
        }

        auditLogService.log("TimeEntry", entry.getId(), "CLOCK_IN", userId, null, null);
        return toDto(entry);
    }

    @Transactional
    public TimeEntryDto clockOut(UUID entryId, ClockOutRequest req, UUID userId) {
        TimeEntry entry = timeEntryRepository.findById(entryId)
                .orElseThrow(() -> BusinessException.notFound("TimeEntry", entryId));

        if (entry.getClockOut() != null) {
            throw BusinessException.badRequest("Already clocked out");
        }

        Instant now = Instant.now();
        entry.setClockOut(now);
        int breakMins = req.getBreakMinutes() != null ? req.getBreakMinutes() : 0;
        entry.setBreakMinutes(breakMins);
        int totalMins = (int) Duration.between(entry.getClockIn(), now).toMinutes() - breakMins;
        entry.setTotalMinutes(Math.max(totalMins, 0));
        if (req.getNotes() != null) entry.setNotes(req.getNotes());
        entry = timeEntryRepository.save(entry);

        // Complete shift if linked
        if (entry.getShiftId() != null) {
            shiftRepository.findById(entry.getShiftId()).ifPresent(shift -> {
                shift.setStatus(ShiftStatus.COMPLETED);
                shiftRepository.save(shift);
            });
        }

        auditLogService.log("TimeEntry", entryId, "CLOCK_OUT", userId, null,
                totalMins + " minutes");
        return toDto(entry);
    }

    public TimeEntryDto getActiveEntry(UUID employeeId) {
        return timeEntryRepository.findByEmployeeIdAndClockOutIsNull(employeeId)
                .map(this::toDto).orElse(null);
    }

    public List<TimeEntryDto> getByEmployee(UUID employeeId, Instant start, Instant end) {
        return timeEntryRepository.findByEmployeeIdAndClockInBetweenOrderByClockInAsc(employeeId, start, end)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public int getTotalMinutes(UUID employeeId, Instant start, Instant end) {
        return timeEntryRepository.sumMinutesByEmployeeBetween(employeeId, start, end);
    }

    private TimeEntryDto toDto(TimeEntry t) {
        return TimeEntryDto.builder()
                .id(t.getId())
                .employeeId(t.getEmployeeId())
                .employeeName(employeeService.getEmployeeName(t.getEmployeeId()))
                .shiftId(t.getShiftId())
                .clockIn(t.getClockIn())
                .clockOut(t.getClockOut())
                .breakMinutes(t.getBreakMinutes())
                .totalMinutes(t.getTotalMinutes())
                .notes(t.getNotes())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
