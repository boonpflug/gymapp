package com.gymplatform.modules.facility.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FacilityDto {
    private UUID id;
    private String name;
    private String description;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String timezone;
    private String phone;
    private String email;
    private String websiteUrl;
    private String openingHours;
    private String logoUrl;
    private String brandColor;
    private String bannerImageUrl;
    private Integer maxOccupancy;
    private UUID parentFacilityId;
    private String parentFacilityName;
    private boolean active;
    private long memberCount;
    private long employeeCount;
    private List<FacilityDto> childFacilities;
    private Instant createdAt;
}
