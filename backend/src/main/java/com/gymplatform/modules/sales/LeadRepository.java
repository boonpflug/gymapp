package com.gymplatform.modules.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    Page<Lead> findByStageIdOrderByCreatedAtDesc(UUID stageId, Pageable pageable);

    List<Lead> findByAssignedStaffIdOrderByCreatedAtDesc(UUID staffId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.stageId = :stageId")
    long countByStageId(@Param("stageId") UUID stageId);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.convertedMemberId IS NOT NULL")
    long countConverted();

    Page<Lead> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
