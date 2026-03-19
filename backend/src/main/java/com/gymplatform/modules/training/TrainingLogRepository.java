package com.gymplatform.modules.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingLogRepository extends JpaRepository<TrainingLog, UUID> {

    List<TrainingLog> findBySessionIdOrderBySetNumberAsc(UUID sessionId);

    @Query("SELECT l FROM TrainingLog l WHERE l.sessionId IN " +
            "(SELECT s.id FROM TrainingSession s WHERE s.memberId = :memberId) " +
            "AND l.exerciseId = :exerciseId ORDER BY l.createdAt ASC")
    List<TrainingLog> findMemberExerciseHistory(
            @Param("memberId") UUID memberId, @Param("exerciseId") UUID exerciseId);

    void deleteBySessionId(UUID sessionId);
}
