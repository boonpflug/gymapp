package com.gymplatform.modules.booking;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.booking.dto.ClassBookingDto;
import com.gymplatform.modules.booking.dto.CreateBookingRequest;
import com.gymplatform.modules.booking.dto.WaitlistEntryDto;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassBookingService {

    private final ClassBookingRepository bookingRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final ClassDefinitionRepository classRepository;
    private final WaitlistEntryRepository waitlistRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public ClassBookingDto book(CreateBookingRequest req, UUID userId) {
        ClassSchedule schedule = scheduleRepository.findById(req.getScheduleId())
                .orElseThrow(() -> BusinessException.notFound("ClassSchedule", req.getScheduleId()));

        if (schedule.isCancelled()) {
            throw BusinessException.badRequest("This class has been cancelled");
        }

        ClassDefinition classDef = classRepository.findById(schedule.getClassId())
                .orElseThrow(() -> BusinessException.notFound("ClassDefinition", schedule.getClassId()));

        // Check booking cutoff
        Instant cutoff = schedule.getStartTime().minusSeconds(classDef.getBookingCutoffMinutes() * 60L);
        if (Instant.now().isAfter(cutoff)) {
            throw BusinessException.badRequest("Booking window has closed for this class");
        }

        // Determine if this is a member or guest booking
        boolean isGuestBooking = req.getMemberId() == null;

        if (isGuestBooking && !classDef.isAllowTrial()) {
            throw BusinessException.badRequest("This class does not allow trial/guest bookings");
        }

        // Check for duplicate member booking
        if (!isGuestBooking) {
            bookingRepository.findByScheduleIdAndMemberIdAndStatus(
                    req.getScheduleId(), req.getMemberId(), BookingStatus.CONFIRMED
            ).ifPresent(existing -> {
                throw BusinessException.conflict("Member already has a confirmed booking for this class");
            });
        }

        // Check capacity
        int capacity = schedule.getCapacityOverride() != null
                ? schedule.getCapacityOverride() : classDef.getCapacity();
        long currentBooked = bookingRepository.countConfirmedByScheduleId(req.getScheduleId());

        if (currentBooked >= capacity) {
            // Class is full — add to waitlist if allowed and this is a member booking
            if (classDef.isAllowWaitlist() && !isGuestBooking) {
                return addToWaitlistAndReturnBookingDto(req, schedule, classDef, userId);
            }
            throw BusinessException.conflict("This class is fully booked");
        }

        // Create booking
        ClassBooking booking = ClassBooking.builder()
                .scheduleId(req.getScheduleId())
                .memberId(req.getMemberId())
                .guestName(req.getGuestName())
                .guestEmail(req.getGuestEmail())
                .guestPhone(req.getGuestPhone())
                .status(BookingStatus.CONFIRMED)
                .bookedAt(Instant.now())
                .tenantId(TenantContext.getTenantId())
                .build();
        booking = bookingRepository.save(booking);

        auditLogService.log("ClassBooking", booking.getId(), "BOOK", userId, null, null);
        publishBookingEvent(booking, classDef, "booking.created");

        return toDto(booking, classDef, schedule);
    }

    @Transactional
    public ClassBookingDto cancel(UUID bookingId, UUID userId) {
        ClassBooking foundBooking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> BusinessException.notFound("ClassBooking", bookingId));

        if (foundBooking.getStatus() != BookingStatus.CONFIRMED) {
            throw BusinessException.badRequest("Only confirmed bookings can be cancelled");
        }

        UUID scheduleId = foundBooking.getScheduleId();
        ClassSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> BusinessException.notFound("ClassSchedule", scheduleId));

        ClassDefinition classDef = classRepository.findById(schedule.getClassId())
                .orElseThrow(() -> BusinessException.notFound("ClassDefinition", schedule.getClassId()));

        // Check cancellation cutoff
        Instant cutoff = schedule.getStartTime().minusSeconds(classDef.getCancellationCutoffMinutes() * 60L);
        if (Instant.now().isAfter(cutoff)) {
            throw BusinessException.badRequest("Cancellation window has closed for this class");
        }

        foundBooking.setStatus(BookingStatus.CANCELLED);
        foundBooking.setCancelledAt(Instant.now());
        ClassBooking booking = bookingRepository.save(foundBooking);

        auditLogService.log("ClassBooking", booking.getId(), "CANCEL", userId, null, null);

        // Promote from waitlist
        promoteFromWaitlist(scheduleId, classDef, schedule, userId);

        return toDto(booking, classDef, schedule);
    }

    @Transactional
    public ClassBookingDto markAttendance(UUID bookingId, BookingStatus status, UUID staffId) {
        if (status != BookingStatus.ATTENDED && status != BookingStatus.NO_SHOW) {
            throw BusinessException.badRequest("Attendance status must be ATTENDED or NO_SHOW");
        }

        ClassBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> BusinessException.notFound("ClassBooking", bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw BusinessException.badRequest("Can only mark attendance on confirmed bookings");
        }

        booking.setStatus(status);
        booking.setAttendanceMarkedAt(Instant.now());
        booking.setAttendanceMarkedBy(staffId);
        booking = bookingRepository.save(booking);

        ClassSchedule schedule = scheduleRepository.findById(booking.getScheduleId()).orElse(null);
        ClassDefinition classDef = schedule != null
                ? classRepository.findById(schedule.getClassId()).orElse(null) : null;

        auditLogService.log("ClassBooking", booking.getId(), "MARK_" + status.name(), staffId, null, null);

        return toDto(booking, classDef, schedule);
    }

    public Page<ClassBookingDto> getMemberBookings(UUID memberId, Pageable pageable) {
        return bookingRepository.findByMemberIdOrderByBookedAtDesc(memberId, pageable)
                .map(b -> {
                    ClassSchedule s = scheduleRepository.findById(b.getScheduleId()).orElse(null);
                    ClassDefinition cd = s != null ? classRepository.findById(s.getClassId()).orElse(null) : null;
                    return toDto(b, cd, s);
                });
    }

    public List<ClassBookingDto> getScheduleBookings(UUID scheduleId) {
        ClassSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> BusinessException.notFound("ClassSchedule", scheduleId));
        ClassDefinition classDef = classRepository.findById(schedule.getClassId()).orElse(null);

        return bookingRepository.findActiveBookingsForSchedule(scheduleId).stream()
                .map(b -> toDto(b, classDef, schedule))
                .collect(Collectors.toList());
    }

    public List<WaitlistEntryDto> getScheduleWaitlist(UUID scheduleId) {
        ClassSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> BusinessException.notFound("ClassSchedule", scheduleId));
        ClassDefinition classDef = classRepository.findById(schedule.getClassId()).orElse(null);

        return waitlistRepository.findByScheduleIdAndStatusOrderByPositionAsc(scheduleId, WaitlistStatus.WAITING)
                .stream().map(w -> toWaitlistDto(w, classDef, schedule))
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelWaitlistEntry(UUID entryId, UUID userId) {
        WaitlistEntry entry = waitlistRepository.findById(entryId)
                .orElseThrow(() -> BusinessException.notFound("WaitlistEntry", entryId));
        if (entry.getStatus() != WaitlistStatus.WAITING) {
            throw BusinessException.badRequest("Only waiting entries can be cancelled");
        }
        entry.setStatus(WaitlistStatus.CANCELLED);
        waitlistRepository.save(entry);
        auditLogService.log("WaitlistEntry", entry.getId(), "CANCEL", userId, null, null);
    }

    private ClassBookingDto addToWaitlistAndReturnBookingDto(CreateBookingRequest req,
                                                              ClassSchedule schedule,
                                                              ClassDefinition classDef,
                                                              UUID userId) {
        // Check if already on waitlist
        waitlistRepository.findByScheduleIdAndMemberIdAndStatus(
                req.getScheduleId(), req.getMemberId(), WaitlistStatus.WAITING
        ).ifPresent(existing -> {
            throw BusinessException.conflict("Member is already on the waitlist for this class");
        });

        int nextPosition = waitlistRepository.findMaxPositionForSchedule(req.getScheduleId()) + 1;

        WaitlistEntry entry = WaitlistEntry.builder()
                .scheduleId(req.getScheduleId())
                .memberId(req.getMemberId())
                .position(nextPosition)
                .joinedAt(Instant.now())
                .status(WaitlistStatus.WAITING)
                .tenantId(TenantContext.getTenantId())
                .build();
        entry = waitlistRepository.save(entry);

        auditLogService.log("WaitlistEntry", entry.getId(), "JOIN_WAITLIST", userId, null,
                "Position: " + nextPosition);

        log.info("Member {} added to waitlist for schedule {} at position {}",
                req.getMemberId(), req.getScheduleId(), nextPosition);

        // Return a DTO indicating waitlisted — use null booking ID to signal waitlist
        String memberName = getMemberName(req.getMemberId());
        return ClassBookingDto.builder()
                .scheduleId(req.getScheduleId())
                .className(classDef != null ? classDef.getName() : null)
                .classStartTime(schedule.getStartTime())
                .memberId(req.getMemberId())
                .memberName(memberName)
                .status(null) // signals waitlisted, not booked
                .bookedAt(entry.getJoinedAt())
                .build();
    }

    private void promoteFromWaitlist(UUID scheduleId, ClassDefinition classDef,
                                      ClassSchedule schedule, UUID userId) {
        List<WaitlistEntry> waiting = waitlistRepository
                .findByScheduleIdAndStatusOrderByPositionAsc(scheduleId, WaitlistStatus.WAITING);
        if (waiting.isEmpty()) return;

        WaitlistEntry next = waiting.get(0);
        next.setStatus(WaitlistStatus.PROMOTED);
        next.setPromotedAt(Instant.now());
        waitlistRepository.save(next);

        // Create a booking for the promoted member
        ClassBooking booking = ClassBooking.builder()
                .scheduleId(scheduleId)
                .memberId(next.getMemberId())
                .status(BookingStatus.CONFIRMED)
                .bookedAt(Instant.now())
                .tenantId(TenantContext.getTenantId())
                .build();
        booking = bookingRepository.save(booking);

        auditLogService.log("ClassBooking", booking.getId(), "WAITLIST_PROMOTE", userId, null,
                "Promoted from waitlist position " + next.getPosition());

        publishBookingEvent(booking, classDef, "booking.created");

        log.info("Promoted member {} from waitlist position {} for schedule {}",
                next.getMemberId(), next.getPosition(), scheduleId);
    }

    private void publishBookingEvent(ClassBooking booking, ClassDefinition classDef, String routingKey) {
        try {
            Map<String, Object> event = Map.of(
                    "bookingId", booking.getId().toString(),
                    "scheduleId", booking.getScheduleId().toString(),
                    "className", classDef != null ? classDef.getName() : "",
                    "memberId", booking.getMemberId() != null ? booking.getMemberId().toString() : "",
                    "tenantId", TenantContext.getTenantId(),
                    "timestamp", Instant.now().toString()
            );
            rabbitTemplate.convertAndSend("member.events", routingKey, event);
        } catch (Exception e) {
            log.warn("Failed to publish booking event: {}", e.getMessage());
        }
    }

    private String getMemberName(UUID memberId) {
        if (memberId == null) return null;
        return memberRepository.findById(memberId)
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse(null);
    }

    private ClassBookingDto toDto(ClassBooking b, ClassDefinition classDef, ClassSchedule schedule) {
        String memberName = b.getMemberId() != null ? getMemberName(b.getMemberId()) : b.getGuestName();

        return ClassBookingDto.builder()
                .id(b.getId())
                .scheduleId(b.getScheduleId())
                .className(classDef != null ? classDef.getName() : null)
                .classStartTime(schedule != null ? schedule.getStartTime() : null)
                .memberId(b.getMemberId())
                .memberName(memberName)
                .guestName(b.getGuestName())
                .guestEmail(b.getGuestEmail())
                .guestPhone(b.getGuestPhone())
                .status(b.getStatus())
                .bookedAt(b.getBookedAt())
                .cancelledAt(b.getCancelledAt())
                .attendanceMarkedAt(b.getAttendanceMarkedAt())
                .build();
    }

    private WaitlistEntryDto toWaitlistDto(WaitlistEntry w, ClassDefinition classDef, ClassSchedule schedule) {
        String memberName = getMemberName(w.getMemberId());
        return WaitlistEntryDto.builder()
                .id(w.getId())
                .scheduleId(w.getScheduleId())
                .className(classDef != null ? classDef.getName() : null)
                .classStartTime(schedule != null ? schedule.getStartTime() : null)
                .memberId(w.getMemberId())
                .memberName(memberName)
                .position(w.getPosition())
                .status(w.getStatus())
                .joinedAt(w.getJoinedAt())
                .promotedAt(w.getPromotedAt())
                .build();
    }
}
