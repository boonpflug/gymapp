package com.gymplatform.modules.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, UUID> {
    List<LoyaltyTier> findByActiveTrueOrderBySortOrderAsc();
    List<LoyaltyTier> findAllByOrderBySortOrderAsc();
    Optional<LoyaltyTier> findFirstByMinPointsLessThanEqualAndActiveTrueOrderByMinPointsDesc(int points);
}
