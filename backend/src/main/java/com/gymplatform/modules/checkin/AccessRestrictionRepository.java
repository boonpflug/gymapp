package com.gymplatform.modules.checkin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccessRestrictionRepository extends JpaRepository<AccessRestriction, UUID> {

    List<AccessRestriction> findByMemberIdAndActiveTrue(UUID memberId);

    List<AccessRestriction> findByMemberId(UUID memberId);

    List<AccessRestriction> findByMemberIdAndReasonAndActiveTrue(UUID memberId, RestrictionReason reason);
}
