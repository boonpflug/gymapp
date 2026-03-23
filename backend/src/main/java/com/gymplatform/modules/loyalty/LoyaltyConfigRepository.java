package com.gymplatform.modules.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyConfigRepository extends JpaRepository<LoyaltyConfig, UUID> {
    Optional<LoyaltyConfig> findByConfigKey(String configKey);
    List<LoyaltyConfig> findAllByOrderByConfigKeyAsc();
}
