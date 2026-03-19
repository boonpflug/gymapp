package com.gymplatform.modules.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    List<Shift> findByEmployeeIdAndStartTimeBetweenOrderByStartTimeAsc(
            UUID employeeId, Instant start, Instant end);

    List<Shift> findByStartTimeBetweenOrderByStartTimeAsc(Instant start, Instant end);

    @Query("SELECT s FROM Shift s WHERE s.employeeId = :employeeId AND s.status = 'SCHEDULED' " +
            "AND s.startTime >= :now ORDER BY s.startTime ASC")
    List<Shift> findUpcomingByEmployee(@Param("employeeId") UUID employeeId, @Param("now") Instant now);

    List<Shift> findByFacilityIdAndStartTimeBetweenOrderByStartTimeAsc(
            UUID facilityId, Instant start, Instant end);
}
