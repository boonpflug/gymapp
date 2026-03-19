package com.gymplatform.modules.sales;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lead extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadSource source;

    @Column(length = 200)
    private String interest;

    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "converted_member_id")
    private UUID convertedMemberId;

    @Column(name = "referral_member_id")
    private UUID referralMemberId;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
