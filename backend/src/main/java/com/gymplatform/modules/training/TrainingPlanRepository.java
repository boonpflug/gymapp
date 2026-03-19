package com.gymplatform.modules.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

    Page<TrainingPlan> findByMemberIdOrderByCreatedAtDesc(UUID memberId, Pageable pageable);

    List<TrainingPlan> findByMemberIdAndStatus(UUID memberId, TrainingPlanStatus status);

    Page<TrainingPlan> findByTemplateTrueOrderByNameAsc(Pageable pageable);

    Page<TrainingPlan> findByCatalogTrueAndStatusOrderByNameAsc(TrainingPlanStatus status, Pageable pageable);

    List<TrainingPlan> findByTrainerIdAndStatusNot(UUID trainerId, TrainingPlanStatus status);
}
