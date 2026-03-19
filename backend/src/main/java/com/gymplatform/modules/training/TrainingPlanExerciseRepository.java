package com.gymplatform.modules.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingPlanExerciseRepository extends JpaRepository<TrainingPlanExercise, UUID> {

    List<TrainingPlanExercise> findByPlanIdOrderBySortOrderAsc(UUID planId);

    void deleteByPlanId(UUID planId);

    int countByPlanId(UUID planId);
}
