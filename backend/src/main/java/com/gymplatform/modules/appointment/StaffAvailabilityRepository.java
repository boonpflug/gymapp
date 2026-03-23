package com.gymplatform.modules.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StaffAvailabilityRepository extends JpaRepository<StaffAvailability, UUID> {
    List<StaffAvailability> findByStaffId(UUID staffId);
    List<StaffAvailability> findByStaffIdAndDayOfWeek(UUID staffId, int dayOfWeek);
    List<StaffAvailability> findByStaffIdAndSpecificDate(UUID staffId, LocalDate specificDate);
    List<StaffAvailability> findByStaffIdAndDayOfWeekAndAvailableTrue(UUID staffId, int dayOfWeek);
}
