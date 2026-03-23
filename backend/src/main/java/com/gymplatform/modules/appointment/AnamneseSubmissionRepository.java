package com.gymplatform.modules.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnamneseSubmissionRepository extends JpaRepository<AnamneseSubmission, UUID> {
    Page<AnamneseSubmission> findByMemberIdOrderBySubmittedAtDesc(UUID memberId, Pageable pageable);
    Page<AnamneseSubmission> findByFormIdOrderBySubmittedAtDesc(UUID formId, Pageable pageable);
    List<AnamneseSubmission> findByAppointmentId(UUID appointmentId);
}
