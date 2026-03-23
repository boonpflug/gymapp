package com.gymplatform.modules.machine;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.machine.dto.*;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MachineService {

    private final MachineRepository machineRepository;
    private final MachineMaintenanceLogRepository maintenanceLogRepository;
    private final MachineSensorSessionRepository sensorSessionRepository;
    private final StrengthMeasurementRepository measurementRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;

    // ── Machines ──────────────────────────────────────────────────────────

    @Transactional
    public MachineDto registerMachine(CreateMachineRequest request, UUID userId) {
        Machine machine = Machine.builder()
                .code(request.getCode())
                .name(request.getName())
                .fullName(request.getFullName())
                .series(request.getSeries())
                .category(request.getCategory())
                .facilityId(request.getFacilityId())
                .serialNumber(request.getSerialNumber())
                .model(request.getModel())
                .firmwareVersion(request.getFirmwareVersion())
                .installationDate(request.getInstallationDate())
                .isComputerAssisted(request.isComputerAssisted())
                .status(MachineStatus.ACTIVE)
                .imageUrl(request.getImageUrl())
                .notes(request.getNotes())
                .tenantId(TenantContext.getTenantId())
                .build();
        machine = machineRepository.save(machine);

        auditLogService.log("Machine", machine.getId(), "CREATED", userId, null, machine.getCode());
        log.info("Machine registered: {} ({})", machine.getCode(), machine.getId());

        return toMachineDto(machine);
    }

    @Transactional(readOnly = true)
    public List<MachineDto> listMachines(UUID facilityId) {
        List<Machine> machines = facilityId != null
                ? machineRepository.findByFacilityIdOrderByCodeAsc(facilityId)
                : machineRepository.findAllByOrderByCodeAsc();
        return machines.stream().map(this::toMachineDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MachineDto getMachine(UUID id) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Machine", id));
        return toMachineDto(machine);
    }

    @Transactional
    public MachineDto updateMachine(UUID id, CreateMachineRequest request, UUID userId) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Machine", id));

        String oldCode = machine.getCode();
        machine.setCode(request.getCode());
        machine.setName(request.getName());
        machine.setFullName(request.getFullName());
        machine.setSeries(request.getSeries());
        machine.setCategory(request.getCategory());
        machine.setFacilityId(request.getFacilityId());
        machine.setSerialNumber(request.getSerialNumber());
        machine.setModel(request.getModel());
        machine.setFirmwareVersion(request.getFirmwareVersion());
        machine.setInstallationDate(request.getInstallationDate());
        machine.setComputerAssisted(request.isComputerAssisted());
        machine.setImageUrl(request.getImageUrl());
        machine.setNotes(request.getNotes());
        machine = machineRepository.save(machine);

        auditLogService.log("Machine", machine.getId(), "UPDATED", userId, oldCode, machine.getCode());
        return toMachineDto(machine);
    }

    @Transactional
    public MachineDto updateMachineStatus(UUID id, MachineStatus status, UUID userId) {
        Machine machine = machineRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Machine", id));

        String oldStatus = machine.getStatus().name();
        machine.setStatus(status);
        machine = machineRepository.save(machine);

        auditLogService.log("Machine", machine.getId(), "STATUS_CHANGED", userId, oldStatus, status.name());
        return toMachineDto(machine);
    }

    @Transactional(readOnly = true)
    public List<MachineDto> getComputerAssistedMachines() {
        return machineRepository.findByIsComputerAssistedTrueOrderByCodeAsc()
                .stream().map(this::toMachineDto).collect(Collectors.toList());
    }

    // ── Maintenance Logs ─────────────────────────────────────────────────

    @Transactional
    public MachineMaintenanceLogDto logMaintenance(CreateMaintenanceLogRequest request, UUID userId) {
        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> BusinessException.notFound("Machine", request.getMachineId()));

        MachineMaintenanceLog logEntry = MachineMaintenanceLog.builder()
                .machineId(request.getMachineId())
                .maintenanceType(request.getMaintenanceType())
                .description(request.getDescription())
                .performedBy(request.getPerformedBy())
                .performedAt(request.getPerformedAt())
                .nextDueDate(request.getNextDueDate())
                .cost(request.getCost())
                .notes(request.getNotes())
                .tenantId(TenantContext.getTenantId())
                .build();
        logEntry = maintenanceLogRepository.save(logEntry);

        machine.setLastMaintenanceDate(request.getPerformedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate());
        if (request.getNextDueDate() != null) {
            machine.setNextMaintenanceDate(request.getNextDueDate());
        }
        machineRepository.save(machine);

        auditLogService.log("MachineMaintenanceLog", logEntry.getId(), "CREATED", userId, null, machine.getCode());
        log.info("Maintenance logged for machine {}: {}", machine.getCode(), request.getMaintenanceType());

        return toMaintenanceLogDto(logEntry, machine.getName());
    }

    @Transactional(readOnly = true)
    public Page<MachineMaintenanceLogDto> getMaintenanceLogs(UUID machineId, Pageable pageable) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> BusinessException.notFound("Machine", machineId));

        return maintenanceLogRepository.findByMachineIdOrderByPerformedAtDesc(machineId, pageable)
                .map(log -> toMaintenanceLogDto(log, machine.getName()));
    }

    // ── Sensor Sessions ──────────────────────────────────────────────────

    @Transactional
    public MachineSensorSessionDto recordSensorSession(RecordSensorSessionRequest request, UUID userId) {
        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> BusinessException.notFound("Machine", request.getMachineId()));
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> BusinessException.notFound("Member", request.getMemberId()));

        MachineSensorSession session = MachineSensorSession.builder()
                .machineId(request.getMachineId())
                .memberId(request.getMemberId())
                .trainingSessionId(request.getTrainingSessionId())
                .trainingLogId(request.getTrainingLogId())
                .startedAt(request.getStartedAt())
                .endedAt(request.getEndedAt())
                .durationSeconds(request.getDurationSeconds())
                .sensorData(request.getSensorData())
                .tenantId(TenantContext.getTenantId())
                .build();
        session = sensorSessionRepository.save(session);

        auditLogService.log("MachineSensorSession", session.getId(), "CREATED", userId, null, machine.getCode());
        return toSensorSessionDto(session, machine, member);
    }

    @Transactional(readOnly = true)
    public Page<MachineSensorSessionDto> getSensorSessions(UUID memberId, UUID machineId, Pageable pageable) {
        if (memberId != null && machineId != null) {
            List<MachineSensorSession> sessions = sensorSessionRepository
                    .findByMemberIdAndMachineIdOrderByStartedAtDesc(memberId, machineId);
            // Convert list to a simple page
            org.springframework.data.domain.PageImpl<MachineSensorSession> page =
                    new org.springframework.data.domain.PageImpl<>(sessions, pageable, sessions.size());
            return page.map(s -> {
                Machine machine = machineRepository.findById(s.getMachineId()).orElse(null);
                Member member = memberRepository.findById(s.getMemberId()).orElse(null);
                return toSensorSessionDto(s, machine, member);
            });
        } else if (memberId != null) {
            return sensorSessionRepository.findByMemberIdOrderByStartedAtDesc(memberId, pageable)
                    .map(s -> {
                        Machine machine = machineRepository.findById(s.getMachineId()).orElse(null);
                        Member member = memberRepository.findById(s.getMemberId()).orElse(null);
                        return toSensorSessionDto(s, machine, member);
                    });
        } else if (machineId != null) {
            return sensorSessionRepository.findByMachineIdOrderByStartedAtDesc(machineId, pageable)
                    .map(s -> {
                        Machine machine = machineRepository.findById(s.getMachineId()).orElse(null);
                        Member member = memberRepository.findById(s.getMemberId()).orElse(null);
                        return toSensorSessionDto(s, machine, member);
                    });
        }
        throw BusinessException.badRequest("At least memberId or machineId must be provided");
    }

    // ── Strength Measurements ────────────────────────────────────────────

    @Transactional
    public StrengthMeasurementDto recordMeasurement(RecordMeasurementRequest request, UUID userId) {
        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> BusinessException.notFound("Machine", request.getMachineId()));
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> BusinessException.notFound("Member", request.getMemberId()));

        StrengthMeasurement measurement = StrengthMeasurement.builder()
                .machineId(request.getMachineId())
                .memberId(request.getMemberId())
                .sensorSessionId(request.getSensorSessionId())
                .measurementType(request.getMeasurementType())
                .peakForceNewtons(request.getPeakForceNewtons())
                .avgForceNewtons(request.getAvgForceNewtons())
                .rangeOfMotionDegrees(request.getRangeOfMotionDegrees())
                .timeUnderTensionSeconds(request.getTimeUnderTensionSeconds())
                .repetitions(request.getRepetitions())
                .setNumber(request.getSetNumber())
                .notes(request.getNotes())
                .measuredAt(request.getMeasuredAt())
                .tenantId(TenantContext.getTenantId())
                .build();
        measurement = measurementRepository.save(measurement);

        auditLogService.log("StrengthMeasurement", measurement.getId(), "CREATED", userId, null, machine.getCode());
        return toMeasurementDto(measurement, machine, member);
    }

    @Transactional(readOnly = true)
    public Page<StrengthMeasurementDto> getMemberMeasurements(UUID memberId, Pageable pageable) {
        return measurementRepository.findByMemberIdOrderByMeasuredAtDesc(memberId, pageable)
                .map(m -> {
                    Machine machine = machineRepository.findById(m.getMachineId()).orElse(null);
                    Member member = memberRepository.findById(m.getMemberId()).orElse(null);
                    return toMeasurementDto(m, machine, member);
                });
    }

    @Transactional(readOnly = true)
    public MemberProgressDto getMemberProgress(UUID memberId, UUID machineId, MeasurementType type) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("Member", memberId));
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> BusinessException.notFound("Machine", machineId));

        List<StrengthMeasurement> measurements = measurementRepository
                .findByMemberIdAndMachineIdAndMeasurementTypeOrderByMeasuredAtAsc(memberId, machineId, type);

        List<StrengthMeasurementDto> measurementDtos = measurements.stream()
                .map(m -> toMeasurementDto(m, machine, member))
                .collect(Collectors.toList());

        BigDecimal initialPeakForce = measurements.isEmpty() ? null : measurements.get(0).getPeakForceNewtons();
        BigDecimal latestPeakForce = measurements.isEmpty() ? null : measurements.get(measurements.size() - 1).getPeakForceNewtons();

        double improvementPercent = 0.0;
        if (initialPeakForce != null && latestPeakForce != null
                && initialPeakForce.compareTo(BigDecimal.ZERO) > 0) {
            improvementPercent = latestPeakForce.subtract(initialPeakForce)
                    .divide(initialPeakForce, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        String memberName = member.getFirstName() + " " + member.getLastName();
        return MemberProgressDto.builder()
                .memberId(memberId)
                .memberName(memberName)
                .machineId(machineId)
                .machineCode(machine.getCode())
                .machineName(machine.getName())
                .measurements(measurementDtos)
                .initialPeakForce(initialPeakForce)
                .latestPeakForce(latestPeakForce)
                .improvementPercent(improvementPercent)
                .build();
    }

    // ── Machine Utilization ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MachineUtilizationDto> getMachineUtilization(UUID facilityId) {
        List<Machine> machines = facilityId != null
                ? machineRepository.findByFacilityIdOrderByCodeAsc(facilityId)
                : machineRepository.findAllByOrderByCodeAsc();

        return machines.stream().map(machine -> {
            List<MachineSensorSession> sessions = sensorSessionRepository
                    .findByMemberIdAndMachineIdOrderByStartedAtDesc(null, machine.getId());
            // Use all sessions for this machine
            Page<MachineSensorSession> sessionPage = sensorSessionRepository
                    .findByMachineIdOrderByStartedAtDesc(machine.getId(), Pageable.unpaged());
            List<MachineSensorSession> allSessions = sessionPage.getContent();

            long totalSessions = allSessions.size();
            long totalMembers = allSessions.stream()
                    .map(MachineSensorSession::getMemberId)
                    .distinct()
                    .count();
            double avgDuration = allSessions.stream()
                    .filter(s -> s.getDurationSeconds() != null)
                    .mapToInt(MachineSensorSession::getDurationSeconds)
                    .average()
                    .orElse(0.0);
            java.time.Instant lastUsedAt = allSessions.stream()
                    .map(MachineSensorSession::getStartedAt)
                    .max(java.time.Instant::compareTo)
                    .orElse(null);

            return MachineUtilizationDto.builder()
                    .machineId(machine.getId())
                    .machineCode(machine.getCode())
                    .machineName(machine.getName())
                    .totalSessions(totalSessions)
                    .totalMembers(totalMembers)
                    .avgSessionDurationSeconds(avgDuration)
                    .lastUsedAt(lastUsedAt)
                    .build();
        }).collect(Collectors.toList());
    }

    // ── Mapping Helpers ──────────────────────────────────────────────────

    private MachineDto toMachineDto(Machine m) {
        return MachineDto.builder()
                .id(m.getId())
                .code(m.getCode())
                .name(m.getName())
                .fullName(m.getFullName())
                .series(m.getSeries())
                .category(m.getCategory())
                .facilityId(m.getFacilityId())
                .serialNumber(m.getSerialNumber())
                .model(m.getModel())
                .firmwareVersion(m.getFirmwareVersion())
                .installationDate(m.getInstallationDate())
                .isComputerAssisted(m.isComputerAssisted())
                .status(m.getStatus())
                .imageUrl(m.getImageUrl())
                .notes(m.getNotes())
                .lastMaintenanceDate(m.getLastMaintenanceDate())
                .nextMaintenanceDate(m.getNextMaintenanceDate())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private MachineMaintenanceLogDto toMaintenanceLogDto(MachineMaintenanceLog l, String machineName) {
        return MachineMaintenanceLogDto.builder()
                .id(l.getId())
                .machineId(l.getMachineId())
                .machineName(machineName)
                .maintenanceType(l.getMaintenanceType())
                .description(l.getDescription())
                .performedBy(l.getPerformedBy())
                .performedAt(l.getPerformedAt())
                .nextDueDate(l.getNextDueDate())
                .cost(l.getCost())
                .notes(l.getNotes())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private MachineSensorSessionDto toSensorSessionDto(MachineSensorSession s, Machine machine, Member member) {
        return MachineSensorSessionDto.builder()
                .id(s.getId())
                .machineId(s.getMachineId())
                .machineCode(machine != null ? machine.getCode() : null)
                .machineName(machine != null ? machine.getName() : null)
                .memberId(s.getMemberId())
                .memberName(member != null ? member.getFirstName() + " " + member.getLastName() : null)
                .trainingSessionId(s.getTrainingSessionId())
                .trainingLogId(s.getTrainingLogId())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .durationSeconds(s.getDurationSeconds())
                .sensorData(s.getSensorData())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private StrengthMeasurementDto toMeasurementDto(StrengthMeasurement m, Machine machine, Member member) {
        return StrengthMeasurementDto.builder()
                .id(m.getId())
                .machineId(m.getMachineId())
                .machineCode(machine != null ? machine.getCode() : null)
                .machineName(machine != null ? machine.getName() : null)
                .memberId(m.getMemberId())
                .memberName(member != null ? member.getFirstName() + " " + member.getLastName() : null)
                .sensorSessionId(m.getSensorSessionId())
                .measurementType(m.getMeasurementType())
                .peakForceNewtons(m.getPeakForceNewtons())
                .avgForceNewtons(m.getAvgForceNewtons())
                .rangeOfMotionDegrees(m.getRangeOfMotionDegrees())
                .timeUnderTensionSeconds(m.getTimeUnderTensionSeconds())
                .repetitions(m.getRepetitions())
                .setNumber(m.getSetNumber())
                .notes(m.getNotes())
                .measuredAt(m.getMeasuredAt())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
