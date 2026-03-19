package com.gymplatform.modules.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadStageRepository extends JpaRepository<LeadStage, UUID> {

    List<LeadStage> findAllByOrderBySortOrderAsc();

    Optional<LeadStage> findByIsDefaultTrue();
}
