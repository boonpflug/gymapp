package com.gymplatform.modules.facility;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberFacilityAccessRepository extends JpaRepository<MemberFacilityAccess, UUID> {

    List<MemberFacilityAccess> findByMemberId(UUID memberId);

    List<MemberFacilityAccess> findByFacilityId(UUID facilityId);

    Optional<MemberFacilityAccess> findByMemberIdAndFacilityId(UUID memberId, UUID facilityId);

    Optional<MemberFacilityAccess> findByMemberIdAndHomeFacilityTrue(UUID memberId);

    boolean existsByMemberIdAndFacilityIdAndCrossFacilityAccessTrue(UUID memberId, UUID facilityId);

    void deleteByMemberId(UUID memberId);

    long countByFacilityId(UUID facilityId);
}
