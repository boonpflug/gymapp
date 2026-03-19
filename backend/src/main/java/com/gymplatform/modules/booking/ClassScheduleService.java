package com.gymplatform.modules.booking;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.booking.dto.ClassScheduleDto;
import com.gymplatform.modules.booking.dto.CreateScheduleRequest;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleService {

    private final ClassScheduleRepository scheduleRepository;
    private final ClassDefinitionRepository classRepository;
    private final ClassCategoryRepository categoryRepository;
    private final ClassBookingRepository bookingRepository;
    private final WaitlistEntryRepository waitlistRepository;

    public List<ClassScheduleDto> getWeeklySchedule(Instant weekStart) {
        Instant weekEnd = weekStart.plus(7, ChronoUnit.DAYS);
        List<ClassSchedule> schedules = scheduleRepository.findSchedulesBetween(weekStart, weekEnd);
        return schedules.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ClassScheduleDto> getScheduleRange(Instant from, Instant to) {
        List<ClassSchedule> schedules = scheduleRepository.findSchedulesBetween(from, to);
        return schedules.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ClassScheduleDto getById(UUID id) {
        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ClassSchedule", id));
        return toDto(schedule);
    }

    public List<ClassScheduleDto> getByTrainer(UUID trainerId, Instant from, Instant to) {
        return scheduleRepository.findByTrainerAndDateRange(trainerId, from, to)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<ClassScheduleDto> create(CreateScheduleRequest req) {
        ClassDefinition classDef = classRepository.findById(req.getClassId())
                .orElseThrow(() -> BusinessException.notFound("ClassDefinition", req.getClassId()));

        String tenantId = TenantContext.getTenantId();
        UUID trainerId = req.getTrainerId() != null ? req.getTrainerId() : classDef.getTrainerId();
        String room = req.getRoom() != null ? req.getRoom() : classDef.getRoom();
        String virtualLink = req.getVirtualLink() != null ? req.getVirtualLink() : classDef.getVirtualLink();
        Duration duration = Duration.ofMinutes(classDef.getDurationMinutes());

        RecurrenceRule rule = req.getRecurrenceRule() != null ? req.getRecurrenceRule() : RecurrenceRule.NONE;
        int weeksToGenerate = rule == RecurrenceRule.NONE ? 1 : req.getRecurrenceWeeks();

        UUID recurrenceGroupId = rule != RecurrenceRule.NONE ? UUID.randomUUID() : null;

        List<ClassSchedule> created = new ArrayList<>();
        Instant currentStart = req.getStartTime();

        for (int i = 0; i < weeksToGenerate; i++) {
            ClassSchedule schedule = ClassSchedule.builder()
                    .classId(req.getClassId())
                    .trainerId(trainerId)
                    .startTime(currentStart)
                    .endTime(currentStart.plus(duration))
                    .room(room)
                    .capacityOverride(req.getCapacityOverride())
                    .virtualLink(virtualLink)
                    .cancelled(false)
                    .recurrenceRule(rule)
                    .recurrenceGroupId(recurrenceGroupId)
                    .tenantId(tenantId)
                    .build();
            created.add(scheduleRepository.save(schedule));

            currentStart = advanceByRule(currentStart, rule);
        }

        return created.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ClassScheduleDto cancelSchedule(UUID id, String reason) {
        ClassSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("ClassSchedule", id));

        if (schedule.isCancelled()) {
            throw BusinessException.badRequest("Schedule is already cancelled");
        }

        schedule.setCancelled(true);
        schedule.setCancellationReason(reason);
        schedule = scheduleRepository.save(schedule);

        // Cancel all confirmed bookings for this schedule
        List<ClassBooking> bookings = bookingRepository.findByScheduleIdAndStatus(id, BookingStatus.CONFIRMED);
        for (ClassBooking booking : bookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(Instant.now());
            bookingRepository.save(booking);
        }

        // Expire all waitlist entries
        List<WaitlistEntry> waitlist = waitlistRepository
                .findByScheduleIdAndStatusOrderByPositionAsc(id, WaitlistStatus.WAITING);
        for (WaitlistEntry entry : waitlist) {
            entry.setStatus(WaitlistStatus.EXPIRED);
            waitlistRepository.save(entry);
        }

        log.info("Cancelled schedule {} with {} bookings and {} waitlist entries",
                id, bookings.size(), waitlist.size());

        return toDto(schedule);
    }

    private Instant advanceByRule(Instant current, RecurrenceRule rule) {
        return switch (rule) {
            case DAILY -> current.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> current.plus(7, ChronoUnit.DAYS);
            case BIWEEKLY -> current.plus(14, ChronoUnit.DAYS);
            case MONTHLY -> current.plus(30, ChronoUnit.DAYS);
            case NONE -> current;
        };
    }

    ClassScheduleDto toDto(ClassSchedule s) {
        ClassDefinition classDef = classRepository.findById(s.getClassId()).orElse(null);
        String className = classDef != null ? classDef.getName() : null;
        String categoryName = null;
        String categoryColor = null;
        if (classDef != null && classDef.getCategoryId() != null) {
            ClassCategory cat = categoryRepository.findById(classDef.getCategoryId()).orElse(null);
            if (cat != null) {
                categoryName = cat.getName();
                categoryColor = cat.getColor();
            }
        }

        int capacity = s.getCapacityOverride() != null ? s.getCapacityOverride()
                : (classDef != null ? classDef.getCapacity() : 0);

        long bookedCount = bookingRepository.countConfirmedByScheduleId(s.getId());
        long waitlistCount = waitlistRepository.countByScheduleIdAndStatus(s.getId(), WaitlistStatus.WAITING);

        return ClassScheduleDto.builder()
                .id(s.getId())
                .classId(s.getClassId())
                .className(className)
                .categoryName(categoryName)
                .categoryColor(categoryColor)
                .trainerId(s.getTrainerId())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .room(s.getRoom())
                .capacity(capacity)
                .bookedCount(bookedCount)
                .waitlistCount(waitlistCount)
                .virtualLink(s.getVirtualLink())
                .cancelled(s.isCancelled())
                .cancellationReason(s.getCancellationReason())
                .recurrenceRule(s.getRecurrenceRule())
                .recurrenceGroupId(s.getRecurrenceGroupId())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
