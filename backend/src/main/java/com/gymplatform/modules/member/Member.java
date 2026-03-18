package com.gymplatform.modules.member;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Member extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "member_number", unique = true)
    private String memberNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @Embedded
    private Address address;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "health_notes", columnDefinition = "TEXT")
    private String healthNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "tenant_id")
    private String tenantId;
}
