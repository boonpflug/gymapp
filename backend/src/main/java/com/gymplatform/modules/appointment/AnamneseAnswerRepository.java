package com.gymplatform.modules.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnamneseAnswerRepository extends JpaRepository<AnamneseAnswer, UUID> {
    List<AnamneseAnswer> findBySubmissionId(UUID submissionId);
    void deleteBySubmissionId(UUID submissionId);
}
