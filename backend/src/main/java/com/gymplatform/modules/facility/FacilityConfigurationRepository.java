package com.gymplatform.modules.facility;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacilityConfigurationRepository extends JpaRepository<FacilityConfiguration, UUID> {

    List<FacilityConfiguration> findByFacilityId(UUID facilityId);

    Optional<FacilityConfiguration> findByFacilityIdAndConfigKey(UUID facilityId, String configKey);

    void deleteByFacilityIdAndConfigKey(UUID facilityId, String configKey);
}
