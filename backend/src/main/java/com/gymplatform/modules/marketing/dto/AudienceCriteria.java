package com.gymplatform.modules.marketing.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AudienceCriteria {
    private List<String> memberStatuses;
    private Integer minCheckInFrequencyDays;
    private Integer maxCheckInFrequencyDays;
    private Integer noCheckInDays;
    private List<String> tags;
    private List<UUID> facilityIds;
    private String contractStatus;
    private Integer contractExpiresWithinDays;
    private String joinedAfter;
    private String joinedBefore;
    private String gender;
    private Integer minAge;
    private Integer maxAge;
}
