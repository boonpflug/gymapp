package com.gymplatform.modules.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnamneseQuestionRepository extends JpaRepository<AnamneseQuestion, UUID> {
    List<AnamneseQuestion> findByFormIdOrderBySortOrderAsc(UUID formId);
    void deleteByFormId(UUID formId);
}
