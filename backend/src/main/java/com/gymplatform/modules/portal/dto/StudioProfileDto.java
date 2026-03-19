package com.gymplatform.modules.portal.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudioProfileDto {
    private String name;
    private String description;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
    private String websiteUrl;
    private String openingHours;
    private String logoUrl;
    private String brandColor;
    private String bannerImageUrl;
    private int facilityCount;
    private List<FacilitySummary> facilities;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FacilitySummary {
        private UUID id;
        private String name;
        private String city;
        private String street;
        private String phone;
        private String openingHours;
    }
}
