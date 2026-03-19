package com.gymplatform.modules.facility;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "member_facility_access")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberFacilityAccess extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "home_facility")
    private boolean homeFacility = false;

    @Column(name = "cross_facility_access")
    private boolean crossFacilityAccess = true;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
