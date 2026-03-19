package com.gymplatform.modules.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassBookingRepository extends JpaRepository<ClassBooking, UUID> {

    List<ClassBooking> findByScheduleIdAndStatusNot(UUID scheduleId, BookingStatus status);

    @Query("SELECT COUNT(b) FROM ClassBooking b WHERE b.scheduleId = :scheduleId AND b.status = 'CONFIRMED'")
    long countConfirmedByScheduleId(@Param("scheduleId") UUID scheduleId);

    Optional<ClassBooking> findByScheduleIdAndMemberIdAndStatus(UUID scheduleId, UUID memberId, BookingStatus status);

    Page<ClassBooking> findByMemberIdOrderByBookedAtDesc(UUID memberId, Pageable pageable);

    List<ClassBooking> findByScheduleIdAndStatus(UUID scheduleId, BookingStatus status);

    @Query("SELECT b FROM ClassBooking b WHERE b.scheduleId = :scheduleId " +
            "AND b.status IN ('CONFIRMED', 'ATTENDED', 'NO_SHOW') ORDER BY b.bookedAt ASC")
    List<ClassBooking> findActiveBookingsForSchedule(@Param("scheduleId") UUID scheduleId);
}
