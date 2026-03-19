package com.gymplatform.modules.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, UUID> {

    List<PromoCodeUsage> findByPromoCodeIdOrderByUsedAtDesc(UUID promoCodeId);

    long countByPromoCodeId(UUID promoCodeId);
}
