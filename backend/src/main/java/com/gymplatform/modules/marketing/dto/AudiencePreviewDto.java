package com.gymplatform.modules.marketing.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AudiencePreviewDto {
    private int totalCount;
    private List<AudienceMemberSummary> sample;

    @Data
    @Builder
    public static class AudienceMemberSummary {
        private UUID memberId;
        private String firstName;
        private String lastName;
        private String email;
        private String status;
    }
}
