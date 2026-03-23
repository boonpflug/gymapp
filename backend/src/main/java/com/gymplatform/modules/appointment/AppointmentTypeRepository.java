package com.gymplatform.modules.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, UUID> {
    List<AppointmentType> findByActiveTrueOrderByNameAsc();
    List<AppointmentType> findAllByOrderByNameAsc();
}
