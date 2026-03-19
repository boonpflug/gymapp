package com.gymplatform.modules.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeFacilityRepository extends JpaRepository<EmployeeFacility, UUID> {

    List<EmployeeFacility> findByEmployeeId(UUID employeeId);

    List<EmployeeFacility> findByFacilityId(UUID facilityId);

    void deleteByEmployeeId(UUID employeeId);
}
