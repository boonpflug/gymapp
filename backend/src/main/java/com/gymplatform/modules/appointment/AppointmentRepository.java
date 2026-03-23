package com.gymplatform.modules.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {
    Page<Appointment> findByMemberIdOrderByStartTimeDesc(UUID memberId, Pageable pageable);
    Page<Appointment> findByStaffIdOrderByStartTimeDesc(UUID staffId, Pageable pageable);
    List<Appointment> findByStaffIdAndStartTimeBetweenOrderByStartTimeAsc(UUID staffId, Instant start, Instant end);
    List<Appointment> findByMemberIdAndStartTimeBetween(UUID memberId, Instant start, Instant end);
    List<Appointment> findByFacilityIdAndStartTimeBetween(UUID facilityId, Instant start, Instant end);
    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);
    long countByStaffIdAndStartTimeBetweenAndStatusNot(UUID staffId, Instant start, Instant end, AppointmentStatus excludeStatus);
}
