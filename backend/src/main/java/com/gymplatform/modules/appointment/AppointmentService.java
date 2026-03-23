package com.gymplatform.modules.appointment;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.appointment.dto.*;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.staff.Employee;
import com.gymplatform.modules.staff.EmployeeRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentTypeRepository appointmentTypeRepository;
    private final StaffAvailabilityRepository staffAvailabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final MemberRepository memberRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    // ── Appointment Types ──────────────────────────────────────────────

    @Transactional
    public AppointmentTypeDto createType(CreateAppointmentTypeRequest request, UUID userId) {
        AppointmentType type = AppointmentType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .color(request.getColor())
                .requiresTrainer(request.isRequiresTrainer())
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        type = appointmentTypeRepository.save(type);

        auditLogService.log("AppointmentType", type.getId(), "CREATED", userId, null, type.getName());
        log.info("Appointment type created: {} by user {}", type.getName(), userId);

        return toTypeDto(type);
    }

    @Transactional(readOnly = true)
    public List<AppointmentTypeDto> listTypes() {
        return appointmentTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toTypeDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentTypeDto updateType(UUID id, CreateAppointmentTypeRequest request, UUID userId) {
        AppointmentType type = appointmentTypeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("AppointmentType", id));

        String oldName = type.getName();
        type.setName(request.getName());
        type.setDescription(request.getDescription());
        type.setDurationMinutes(request.getDurationMinutes());
        type.setColor(request.getColor());
        type.setRequiresTrainer(request.isRequiresTrainer());
        type = appointmentTypeRepository.save(type);

        auditLogService.log("AppointmentType", type.getId(), "UPDATED", userId, oldName, type.getName());
        log.info("Appointment type updated: {} by user {}", type.getName(), userId);

        return toTypeDto(type);
    }

    @Transactional
    public void deactivateType(UUID id, UUID userId) {
        AppointmentType type = appointmentTypeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("AppointmentType", id));

        type.setActive(false);
        appointmentTypeRepository.save(type);

        auditLogService.log("AppointmentType", type.getId(), "DEACTIVATED", userId, "active", "inactive");
        log.info("Appointment type deactivated: {} by user {}", type.getName(), userId);
    }

    // ── Staff Availability ─────────────────────────────────────────────

    @Transactional
    public StaffAvailabilityDto setAvailability(CreateAvailabilityRequest request, UUID userId) {
        employeeRepository.findById(request.getStaffId())
                .orElseThrow(() -> BusinessException.notFound("Employee", request.getStaffId()));

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw BusinessException.badRequest("Start time must be before end time");
        }

        StaffAvailability availability = StaffAvailability.builder()
                .staffId(request.getStaffId())
                .facilityId(request.getFacilityId())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .recurring(request.isRecurring())
                .specificDate(request.getSpecificDate())
                .available(request.isAvailable())
                .tenantId(TenantContext.getTenantId())
                .build();
        availability = staffAvailabilityRepository.save(availability);

        auditLogService.log("StaffAvailability", availability.getId(), "CREATED", userId, null, null);
        log.info("Staff availability created for staff {} by user {}", request.getStaffId(), userId);

        return toAvailabilityDto(availability);
    }

    @Transactional(readOnly = true)
    public List<StaffAvailabilityDto> getStaffAvailability(UUID staffId) {
        return staffAvailabilityRepository.findByStaffId(staffId).stream()
                .map(this::toAvailabilityDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAvailability(UUID id, UUID userId) {
        StaffAvailability availability = staffAvailabilityRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("StaffAvailability", id));

        staffAvailabilityRepository.delete(availability);

        auditLogService.log("StaffAvailability", id, "DELETED", userId, null, null);
        log.info("Staff availability deleted: {} by user {}", id, userId);
    }

    // ── Appointments ───────────────────────────────────────────────────

    @Transactional
    public AppointmentDto createAppointment(CreateAppointmentRequest request, UUID userId) {
        // Validate member exists
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> BusinessException.notFound("Member", request.getMemberId()));

        // Validate appointment type exists
        AppointmentType type = appointmentTypeRepository.findById(request.getAppointmentTypeId())
                .orElseThrow(() -> BusinessException.notFound("AppointmentType", request.getAppointmentTypeId()));

        // Validate staff exists if provided
        Employee staff = null;
        if (request.getStaffId() != null) {
            staff = employeeRepository.findById(request.getStaffId())
                    .orElseThrow(() -> BusinessException.notFound("Employee", request.getStaffId()));
        }

        // Calculate end time from type duration
        Instant endTime = request.getStartTime().plus(Duration.ofMinutes(type.getDurationMinutes()));

        // Check for overlapping appointments for the staff
        if (request.getStaffId() != null) {
            long overlapping = appointmentRepository.countByStaffIdAndStartTimeBetweenAndStatusNot(
                    request.getStaffId(),
                    request.getStartTime(),
                    endTime,
                    AppointmentStatus.CANCELLED
            );
            if (overlapping > 0) {
                throw BusinessException.badRequest("Staff member has an overlapping appointment during this time slot");
            }

            // Check staff availability for the requested day/time
            validateStaffAvailability(request.getStaffId(), request.getStartTime(), endTime);
        }

        Appointment appointment = Appointment.builder()
                .memberId(request.getMemberId())
                .staffId(request.getStaffId())
                .facilityId(request.getFacilityId())
                .appointmentTypeId(request.getAppointmentTypeId())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.getNotes())
                .recurringRule(request.getRecurringRule())
                .tenantId(TenantContext.getTenantId())
                .build();
        appointment = appointmentRepository.save(appointment);

        auditLogService.log("Appointment", appointment.getId(), "CREATED", userId, null,
                "Member: " + member.getFirstName() + " " + member.getLastName());
        log.info("Appointment created: {} for member {} by user {}", appointment.getId(), request.getMemberId(), userId);

        return toAppointmentDto(appointment, member, staff, type);
    }

    @Transactional
    public AppointmentDto confirmAppointment(UUID id, UUID userId) {
        return updateAppointmentStatus(id, AppointmentStatus.CONFIRMED, userId);
    }

    @Transactional
    public AppointmentDto startAppointment(UUID id, UUID userId) {
        return updateAppointmentStatus(id, AppointmentStatus.IN_PROGRESS, userId);
    }

    @Transactional
    public AppointmentDto completeAppointment(UUID id, UUID userId) {
        return updateAppointmentStatus(id, AppointmentStatus.COMPLETED, userId);
    }

    @Transactional
    public AppointmentDto cancelAppointment(UUID id, String reason, UUID userId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Appointment", id));

        String oldStatus = appointment.getStatus().name();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        appointment.setCancelledAt(Instant.now());
        appointment = appointmentRepository.save(appointment);

        auditLogService.log("Appointment", id, "CANCELLED", userId, oldStatus, "CANCELLED");
        log.info("Appointment cancelled: {} by user {}", id, userId);

        return toAppointmentDto(appointment);
    }

    @Transactional
    public AppointmentDto markNoShow(UUID id, UUID userId) {
        return updateAppointmentStatus(id, AppointmentStatus.NO_SHOW, userId);
    }

    @Transactional(readOnly = true)
    public AppointmentDto getAppointment(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Appointment", id));
        return toAppointmentDto(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> getMemberAppointments(UUID memberId, Pageable pageable) {
        return appointmentRepository.findByMemberIdOrderByStartTimeDesc(memberId, pageable)
                .map(this::toAppointmentDto);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentDto> getStaffAppointments(UUID staffId, Pageable pageable) {
        return appointmentRepository.findByStaffIdOrderByStartTimeDesc(staffId, pageable)
                .map(this::toAppointmentDto);
    }

    @Transactional(readOnly = true)
    public DayAgendaDto getStaffAgenda(UUID staffId, LocalDate date) {
        Employee staff = employeeRepository.findById(staffId)
                .orElseThrow(() -> BusinessException.notFound("Employee", staffId));

        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Appointment> appointments = appointmentRepository
                .findByStaffIdAndStartTimeBetweenOrderByStartTimeAsc(staffId, dayStart, dayEnd);

        List<AppointmentDto> appointmentDtos = appointments.stream()
                .map(this::toAppointmentDto)
                .collect(Collectors.toList());

        return DayAgendaDto.builder()
                .staffId(staffId)
                .staffName(staff.getFirstName() + " " + staff.getLastName())
                .date(date)
                .appointments(appointmentDtos)
                .totalAppointments(appointmentDtos.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> getUpcomingAppointments(UUID staffId, int days) {
        Instant now = Instant.now();
        Instant until = LocalDate.now().plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant();

        return appointmentRepository
                .findByStaffIdAndStartTimeBetweenOrderByStartTimeAsc(staffId, now, until)
                .stream()
                .map(this::toAppointmentDto)
                .collect(Collectors.toList());
    }

    // ── Private helpers ────────────────────────────────────────────────

    private void validateStaffAvailability(UUID staffId, Instant startTime, Instant endTime) {
        LocalDateTime startLdt = LocalDateTime.ofInstant(startTime, ZoneOffset.UTC);
        LocalTime appointmentStartTime = startLdt.toLocalTime();
        LocalTime appointmentEndTime = LocalDateTime.ofInstant(endTime, ZoneOffset.UTC).toLocalTime();
        int dayOfWeek = startLdt.getDayOfWeek().getValue();
        LocalDate date = startLdt.toLocalDate();

        // Check specific date availability first
        List<StaffAvailability> specificSlots = staffAvailabilityRepository
                .findByStaffIdAndSpecificDate(staffId, date);
        if (!specificSlots.isEmpty()) {
            boolean covered = specificSlots.stream()
                    .filter(StaffAvailability::isAvailable)
                    .anyMatch(s -> !appointmentStartTime.isBefore(s.getStartTime())
                            && !appointmentEndTime.isAfter(s.getEndTime()));
            if (!covered) {
                throw BusinessException.badRequest("Staff member is not available at the requested time");
            }
            return;
        }

        // Fall back to recurring availability
        List<StaffAvailability> recurringSlots = staffAvailabilityRepository
                .findByStaffIdAndDayOfWeekAndAvailableTrue(staffId, dayOfWeek);
        if (recurringSlots.isEmpty()) {
            throw BusinessException.badRequest("Staff member has no availability on the requested day");
        }

        boolean covered = recurringSlots.stream()
                .anyMatch(s -> !appointmentStartTime.isBefore(s.getStartTime())
                        && !appointmentEndTime.isAfter(s.getEndTime()));
        if (!covered) {
            throw BusinessException.badRequest("Staff member is not available at the requested time");
        }
    }

    private AppointmentDto updateAppointmentStatus(UUID id, AppointmentStatus newStatus, UUID userId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Appointment", id));

        String oldStatus = appointment.getStatus().name();
        appointment.setStatus(newStatus);
        appointment = appointmentRepository.save(appointment);

        auditLogService.log("Appointment", id, "STATUS_CHANGED", userId, oldStatus, newStatus.name());
        log.info("Appointment {} status changed from {} to {} by user {}", id, oldStatus, newStatus, userId);

        return toAppointmentDto(appointment);
    }

    private AppointmentDto toAppointmentDto(Appointment appointment) {
        String memberName = memberRepository.findById(appointment.getMemberId())
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse(null);

        String staffName = null;
        if (appointment.getStaffId() != null) {
            staffName = employeeRepository.findById(appointment.getStaffId())
                    .map(e -> e.getFirstName() + " " + e.getLastName())
                    .orElse(null);
        }

        String typeName = appointmentTypeRepository.findById(appointment.getAppointmentTypeId())
                .map(AppointmentType::getName)
                .orElse(null);

        return AppointmentDto.builder()
                .id(appointment.getId())
                .memberId(appointment.getMemberId())
                .memberName(memberName)
                .staffId(appointment.getStaffId())
                .staffName(staffName)
                .facilityId(appointment.getFacilityId())
                .appointmentTypeId(appointment.getAppointmentTypeId())
                .appointmentTypeName(typeName)
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .cancellationReason(appointment.getCancellationReason())
                .cancelledAt(appointment.getCancelledAt())
                .recurringRule(appointment.getRecurringRule())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    private AppointmentDto toAppointmentDto(Appointment appointment, Member member, Employee staff, AppointmentType type) {
        return AppointmentDto.builder()
                .id(appointment.getId())
                .memberId(appointment.getMemberId())
                .memberName(member.getFirstName() + " " + member.getLastName())
                .staffId(appointment.getStaffId())
                .staffName(staff != null ? staff.getFirstName() + " " + staff.getLastName() : null)
                .facilityId(appointment.getFacilityId())
                .appointmentTypeId(appointment.getAppointmentTypeId())
                .appointmentTypeName(type.getName())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .cancellationReason(appointment.getCancellationReason())
                .cancelledAt(appointment.getCancelledAt())
                .recurringRule(appointment.getRecurringRule())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    private AppointmentTypeDto toTypeDto(AppointmentType type) {
        return AppointmentTypeDto.builder()
                .id(type.getId())
                .name(type.getName())
                .description(type.getDescription())
                .durationMinutes(type.getDurationMinutes())
                .color(type.getColor())
                .requiresTrainer(type.isRequiresTrainer())
                .active(type.isActive())
                .createdAt(type.getCreatedAt())
                .build();
    }

    private StaffAvailabilityDto toAvailabilityDto(StaffAvailability availability) {
        return StaffAvailabilityDto.builder()
                .id(availability.getId())
                .staffId(availability.getStaffId())
                .facilityId(availability.getFacilityId())
                .dayOfWeek(availability.getDayOfWeek())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .recurring(availability.isRecurring())
                .specificDate(availability.getSpecificDate())
                .available(availability.isAvailable())
                .createdAt(availability.getCreatedAt())
                .build();
    }
}
