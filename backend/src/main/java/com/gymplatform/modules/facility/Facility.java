package com.gymplatform.modules.facility;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "facilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Facility extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 255)
    private String street;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(length = 50)
    private String timezone;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "opening_hours", columnDefinition = "text")
    private String openingHours;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "brand_color", length = 7)
    private String brandColor;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(name = "max_occupancy")
    private Integer maxOccupancy;

    @Column(name = "parent_facility_id")
    private UUID parentFacilityId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
