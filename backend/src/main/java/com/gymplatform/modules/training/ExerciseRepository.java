package com.gymplatform.modules.training;

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
public interface ExerciseRepository extends JpaRepository<Exercise, UUID>, JpaSpecificationExecutor<Exercise> {

    Page<Exercise> findByActiveTrue(Pageable pageable);

    @Query("SELECT e FROM Exercise e WHERE e.active = true AND (e.global = true OR e.tenantId = :tenantId)")
    Page<Exercise> findAvailableExercises(@Param("tenantId") String tenantId, Pageable pageable);

    List<Exercise> findByPrimaryMuscleGroupAndActiveTrue(MuscleGroup muscleGroup);

    List<Exercise> findByExerciseTypeAndActiveTrue(ExerciseType exerciseType);

    @Query("SELECT DISTINCT e.equipment FROM Exercise e WHERE e.equipment IS NOT NULL AND e.active = true ORDER BY e.equipment")
    List<String> findDistinctEquipment();

    @Query("SELECT e FROM Exercise e WHERE e.active = true AND LOWER(e.name) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY " +
            "CASE WHEN LOWER(e.name) = LOWER(:term) THEN 0 " +
            "WHEN LOWER(e.name) LIKE LOWER(CONCAT(:term, '%')) THEN 1 " +
            "ELSE 2 END, LENGTH(e.name)")
    List<Exercise> suggestByName(@Param("term") String term, Pageable pageable);
}
