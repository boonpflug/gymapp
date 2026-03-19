package com.gymplatform.modules.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {

    List<WaitlistEntry> findByScheduleIdAndStatusOrderByPositionAsc(UUID scheduleId, WaitlistStatus status);

    Optional<WaitlistEntry> findByScheduleIdAndMemberIdAndStatus(UUID scheduleId, UUID memberId, WaitlistStatus status);

    @Query("SELECT COALESCE(MAX(w.position), 0) FROM WaitlistEntry w " +
            "WHERE w.scheduleId = :scheduleId AND w.status = 'WAITING'")
    int findMaxPositionForSchedule(@Param("scheduleId") UUID scheduleId);

    long countByScheduleIdAndStatus(UUID scheduleId, WaitlistStatus status);
}
