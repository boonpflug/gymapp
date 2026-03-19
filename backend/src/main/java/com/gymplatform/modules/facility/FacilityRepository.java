package com.gymplatform.modules.facility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {

    Page<Facility> findByActiveTrueOrderByNameAsc(Pageable pageable);

    List<Facility> findByActiveTrueOrderByNameAsc();

    List<Facility> findByParentFacilityId(UUID parentFacilityId);

    long countByActiveTrue();
}
