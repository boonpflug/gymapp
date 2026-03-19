package com.gymplatform.modules.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {

    List<TimeEntry> findByEmployeeIdAndClockInBetweenOrderByClockInAsc(
            UUID employeeId, Instant start, Instant end);

    Optional<TimeEntry> findByEmployeeIdAndClockOutIsNull(UUID employeeId);

    @Query("SELECT COALESCE(SUM(t.totalMinutes), 0) FROM TimeEntry t WHERE t.employeeId = :employeeId " +
            "AND t.clockIn >= :start AND t.clockIn < :end")
    int sumMinutesByEmployeeBetween(@Param("employeeId") UUID employeeId,
                                     @Param("start") Instant start, @Param("end") Instant end);
}
