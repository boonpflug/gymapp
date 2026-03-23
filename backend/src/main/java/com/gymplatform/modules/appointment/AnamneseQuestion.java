package com.gymplatform.modules.appointment;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "anamnese_questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnamneseQuestion extends BaseEntity {

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(length = 100)
    private String section;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
