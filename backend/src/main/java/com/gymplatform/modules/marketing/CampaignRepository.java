package com.gymplatform.modules.marketing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID>,
        JpaSpecificationExecutor<Campaign> {

    Page<Campaign> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Campaign> findByStatusAndScheduledAtLessThanEqual(CampaignStatus status, Instant now);

    Page<Campaign> findByStatusOrderByCreatedAtDesc(CampaignStatus status, Pageable pageable);
}
