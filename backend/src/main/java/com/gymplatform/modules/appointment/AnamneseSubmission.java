package com.gymplatform.modules.appointment;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anamnese_submissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnamneseSubmission extends BaseEntity {

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
