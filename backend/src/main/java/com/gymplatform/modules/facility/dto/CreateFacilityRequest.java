package com.gymplatform.modules.facility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateFacilityRequest {
    @NotBlank
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
}
