package com.gymplatform.modules.sales.dto;

import com.gymplatform.modules.sales.LeadSource;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class LeadDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LeadSource source;
    private String interest;
    private UUID stageId;
    private String stageName;
    private String stageColor;
    private UUID assignedStaffId;
    private String assignedStaffName;
    private String notes;
    private UUID convertedMemberId;
    private UUID referralMemberId;
    private int activityCount;
    private Instant createdAt;
    private Instant updatedAt;
}
