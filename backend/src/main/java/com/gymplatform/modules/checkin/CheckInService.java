package com.gymplatform.modules.checkin;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.checkin.dto.*;
import com.gymplatform.modules.contract.Contract;
import com.gymplatform.modules.contract.ContractRepository;
import com.gymplatform.modules.contract.ContractStatus;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.member.MemberStatus;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {

    private static final int DOUBLE_CHECKIN_WINDOW_MINUTES = 15;
    private static final int OCCUPANCY_WINDOW_HOURS = 18;

    private final CheckInRepository checkInRepository;
    private final AccessDeviceRepository deviceRepository;
    private final MemberRepository memberRepository;
    private final ContractRepository contractRepository;
    private final AccessRestrictionService restrictionService;
    private final OccupancyService occupancyService;
    private final AccessEventRepository eventRepository;
    private final AuditLogService auditLogService;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final List<AccessControlAdapter> accessControlAdapters;

    @Transactional
    public CheckInDto manualCheckIn(ManualCheckInRequest request, UUID staffId) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> BusinessException.notFound("Member", request.getMemberId()));

        String tenantId = TenantContext.getTenantId();

        // Validate access
        String denialReason = validateAccess(member);

        CheckInStatus status = denialReason == null ? CheckInStatus.SUCCESS : CheckInStatus.DENIED;

        CheckIn checkIn = CheckIn.builder()
                .memberId(member.getId())
                .deviceId(request.getDeviceId())
                .method(CheckInMethod.MANUAL)
                .status(status)
                .denialReason(denialReason)
                .staffId(staffId)
                .checkInTime(Instant.now())
                .tenantId(tenantId)
                .build();

        checkIn = checkInRepository.save(checkIn);

        // Log access event
        logAccessEvent(null, member.getId(),
                status == CheckInStatus.SUCCESS ? AccessEventType.CHECK_IN_SUCCESS : AccessEventType.CHECK_IN_DENIED,
                denialReason, tenantId);

        if (status == CheckInStatus.SUCCESS) {
            publishCheckInEvent(checkIn, member, tenantId);
            occupancyService.broadcastOccupancyUpdate(tenantId, getMaxCapacity());
        }

        auditLogService.log("CheckIn", checkIn.getId(), "CHECK_IN",
                staffId, null, "Manual check-in: " + status);

        return toDto(checkIn, member, null);
    }

    @Transactional
    public CheckInDto deviceCheckIn(DeviceCheckInRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> BusinessException.notFound("Member", request.getMemberId()));

        AccessDevice device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> BusinessException.notFound("Device", request.getDeviceId()));

        String tenantId = TenantContext.getTenantId();
        String denialReason = validateAccess(member);

        // If access allowed, communicate with physical device
        if (denialReason == null) {
            AccessControlAdapter adapter = findAdapter(device.getDeviceType());
            if (adapter != null) {
                CheckInResult result = adapter.processCheckIn(member.getId(), device.getId());
                if (!result.isAllowed()) {
                    denialReason = result.getDenialReason();
                }
            }
        }

        CheckInStatus status = denialReason == null ? CheckInStatus.SUCCESS : CheckInStatus.DENIED;

        CheckIn checkIn = CheckIn.builder()
                .memberId(member.getId())
                .deviceId(device.getId())
                .method(request.getMethod())
                .status(status)
                .denialReason(denialReason)
                .checkInTime(Instant.now())
                .tenantId(tenantId)
                .build();

        checkIn = checkInRepository.save(checkIn);

        logAccessEvent(device.getId(), member.getId(),
                status == CheckInStatus.SUCCESS ? AccessEventType.CHECK_IN_SUCCESS : AccessEventType.CHECK_IN_DENIED,
                denialReason, tenantId);

        if (status == CheckInStatus.SUCCESS) {
            publishCheckInEvent(checkIn, member, tenantId);
            occupancyService.broadcastOccupancyUpdate(tenantId, device.getMaxOccupancy());
        }

        return toDto(checkIn, member, device);
    }

    @Transactional
    public void checkOut(CheckOutRequest request) {
        Instant since = Instant.now().minus(OCCUPANCY_WINDOW_HOURS, ChronoUnit.HOURS);
        CheckIn activeCheckIn = checkInRepository.findActiveCheckIn(request.getMemberId(), since)
                .orElseThrow(() -> BusinessException.notFound("Active check-in", request.getMemberId()));

        activeCheckIn.setCheckOutTime(Instant.now());
        checkInRepository.save(activeCheckIn);

        String tenantId = TenantContext.getTenantId();

        // Notify device if applicable
        if (activeCheckIn.getDeviceId() != null) {
            AccessDevice device = deviceRepository.findById(activeCheckIn.getDeviceId()).orElse(null);
            if (device != null) {
                AccessControlAdapter adapter = findAdapter(device.getDeviceType());
                if (adapter != null) {
                    adapter.processCheckOut(request.getMemberId(), device.getId());
                }
            }
        }

        logAccessEvent(activeCheckIn.getDeviceId(), request.getMemberId(),
                AccessEventType.CHECK_OUT, null, tenantId);

        occupancyService.broadcastOccupancyUpdate(tenantId, getMaxCapacity());
    }

    public Page<CheckInDto> getCheckInHistory(UUID memberId, Pageable pageable) {
        return checkInRepository.findByMemberIdOrderByCheckInTimeDesc(memberId, pageable)
                .map(ci -> {
                    Member member = memberRepository.findById(ci.getMemberId()).orElse(null);
                    AccessDevice device = ci.getDeviceId() != null
                            ? deviceRepository.findById(ci.getDeviceId()).orElse(null) : null;
                    return toDto(ci, member, device);
                });
    }

    public Page<CheckInDto> getRecentCheckIns(Pageable pageable) {
        return checkInRepository.findAllByOrderByCheckInTimeDesc(pageable)
                .map(ci -> {
                    Member member = memberRepository.findById(ci.getMemberId()).orElse(null);
                    AccessDevice device = ci.getDeviceId() != null
                            ? deviceRepository.findById(ci.getDeviceId()).orElse(null) : null;
                    return toDto(ci, member, device);
                });
    }

    public Page<CheckInDto> getCurrentlyCheckedIn(Pageable pageable) {
        Instant since = Instant.now().minus(OCCUPANCY_WINDOW_HOURS, ChronoUnit.HOURS);
        return checkInRepository.findCurrentlyCheckedIn(since, pageable)
                .map(ci -> {
                    Member member = memberRepository.findById(ci.getMemberId()).orElse(null);
                    AccessDevice device = ci.getDeviceId() != null
                            ? deviceRepository.findById(ci.getDeviceId()).orElse(null) : null;
                    return toDto(ci, member, device);
                });
    }

    private String validateAccess(Member member) {
        // 1. Member must be active
        if (member.getStatus() != MemberStatus.ACTIVE) {
            return "Member account is " + member.getStatus();
        }

        // 2. Check access restrictions (payment overdue, manual block, etc.)
        if (restrictionService.hasActiveRestriction(member.getId())) {
            return restrictionService.getActiveRestrictionReason(member.getId());
        }

        // 3. Must have an active contract
        List<Contract> activeContracts = contractRepository.findByMemberIdAndStatus(
                member.getId(), ContractStatus.ACTIVE);
        if (activeContracts.isEmpty()) {
            return "No active contract";
        }

        // 4. Double check-in prevention
        Instant doubleCheckInWindow = Instant.now().minus(DOUBLE_CHECKIN_WINDOW_MINUTES, ChronoUnit.MINUTES);
        if (checkInRepository.findActiveCheckIn(member.getId(), doubleCheckInWindow).isPresent()) {
            return "Already checked in within the last " + DOUBLE_CHECKIN_WINDOW_MINUTES + " minutes";
        }

        // 5. Occupancy limit (use global max for manual check-in)
        Integer maxCapacity = getMaxCapacity();
        if (occupancyService.isAtCapacity(maxCapacity)) {
            return "Facility is at maximum capacity";
        }

        return null;
    }

    private Integer getMaxCapacity() {
        // Use the max occupancy from any active device as the facility capacity
        return deviceRepository.findByActiveTrue().stream()
                .map(AccessDevice::getMaxOccupancy)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private AccessControlAdapter findAdapter(DeviceType deviceType) {
        return accessControlAdapters.stream()
                .filter(a -> a.supportsDevice(deviceType))
                .findFirst()
                .orElse(null);
    }

    private void logAccessEvent(UUID deviceId, UUID memberId, AccessEventType eventType,
                                String reasonCode, String tenantId) {
        AccessEvent event = AccessEvent.builder()
                .deviceId(deviceId)
                .memberId(memberId)
                .eventType(eventType)
                .reasonCode(reasonCode)
                .tenantId(tenantId)
                .build();
        eventRepository.save(event);

        // Broadcast event via WebSocket for live event monitor
        messagingTemplate.convertAndSend(
                "/topic/access-events/" + tenantId,
                AccessEventDto.builder()
                        .id(event.getId())
                        .deviceId(deviceId)
                        .memberId(memberId)
                        .eventType(eventType)
                        .reasonCode(reasonCode)
                        .createdAt(event.getCreatedAt())
                        .build()
        );
    }

    private void publishCheckInEvent(CheckIn checkIn, Member member, String tenantId) {
        Map<String, Object> event = Map.of(
                "checkInId", checkIn.getId().toString(),
                "memberId", member.getId().toString(),
                "memberName", member.getFirstName() + " " + member.getLastName(),
                "method", checkIn.getMethod().name(),
                "tenantId", tenantId,
                "timestamp", checkIn.getCheckInTime().toString()
        );
        rabbitTemplate.convertAndSend("member.events", "member.checkedin", event);
    }

    private CheckInDto toDto(CheckIn ci, Member member, AccessDevice device) {
        return CheckInDto.builder()
                .id(ci.getId())
                .memberId(ci.getMemberId())
                .memberName(member != null ? member.getFirstName() + " " + member.getLastName() : null)
                .memberNumber(member != null ? member.getMemberNumber() : null)
                .deviceId(ci.getDeviceId())
                .deviceName(device != null ? device.getName() : null)
                .method(ci.getMethod())
                .status(ci.getStatus())
                .denialReason(ci.getDenialReason())
                .staffId(ci.getStaffId())
                .checkInTime(ci.getCheckInTime())
                .checkOutTime(ci.getCheckOutTime())
                .build();
    }
}
