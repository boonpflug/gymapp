package com.gymplatform.modules.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, UUID> {

    @Query("SELECT s FROM ClassSchedule s WHERE s.startTime >= :from AND s.startTime < :to " +
            "AND s.cancelled = false ORDER BY s.startTime ASC")
    List<ClassSchedule> findSchedulesBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT s FROM ClassSchedule s WHERE s.classId = :classId AND s.startTime >= :from " +
            "AND s.cancelled = false ORDER BY s.startTime ASC")
    List<ClassSchedule> findByClassIdAndStartTimeAfter(@Param("classId") UUID classId,
                                                        @Param("from") Instant from);

    Page<ClassSchedule> findByClassIdOrderByStartTimeDesc(UUID classId, Pageable pageable);

    List<ClassSchedule> findByRecurrenceGroupId(UUID recurrenceGroupId);

    @Query("SELECT s FROM ClassSchedule s WHERE s.trainerId = :trainerId AND s.startTime >= :from " +
            "AND s.startTime < :to AND s.cancelled = false ORDER BY s.startTime ASC")
    List<ClassSchedule> findByTrainerAndDateRange(@Param("trainerId") UUID trainerId,
                                                   @Param("from") Instant from,
                                                   @Param("to") Instant to);
}
