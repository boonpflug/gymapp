package com.gymplatform.modules.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, UUID> {
    List<MembershipTier> findByActiveTrue();
}
