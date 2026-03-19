package com.gymplatform.modules.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingGoalRepository extends JpaRepository<TrainingGoal, UUID> {

    List<TrainingGoal> findByMemberIdAndStatusOrderByCreatedAtDesc(UUID memberId, GoalStatus status);

    List<TrainingGoal> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
}
