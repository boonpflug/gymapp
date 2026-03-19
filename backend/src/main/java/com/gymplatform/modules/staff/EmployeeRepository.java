package com.gymplatform.modules.staff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    Page<Employee> findByActiveTrueOrderByLastNameAsc(Pageable pageable);

    List<Employee> findByEmploymentTypeAndActiveTrue(EmploymentType employmentType);

    Optional<Employee> findByUserId(UUID userId);

    List<Employee> findByActiveTrueOrderByLastNameAsc();
}
