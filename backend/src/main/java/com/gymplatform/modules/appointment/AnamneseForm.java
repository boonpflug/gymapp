package com.gymplatform.modules.appointment;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "anamnese_forms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnamneseForm extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
