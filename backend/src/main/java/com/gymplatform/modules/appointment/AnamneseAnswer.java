package com.gymplatform.modules.appointment;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "anamnese_answers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnamneseAnswer extends BaseEntity {

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "answer_number")
    private BigDecimal answerNumber;

    @Column(name = "answer_boolean")
    private Boolean answerBoolean;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
