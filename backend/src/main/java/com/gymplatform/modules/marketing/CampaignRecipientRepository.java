package com.gymplatform.modules.marketing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, UUID> {

    Page<CampaignRecipient> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);

    List<CampaignRecipient> findByCampaignId(UUID campaignId);

    long countByCampaignId(UUID campaignId);

    long countByCampaignIdAndStatus(UUID campaignId, CampaignEventType status);

    @Query("SELECT cr.status, COUNT(cr) FROM CampaignRecipient cr WHERE cr.campaignId = :campaignId GROUP BY cr.status")
    List<Object[]> countByStatusGrouped(@Param("campaignId") UUID campaignId);
}
