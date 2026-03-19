package com.gymplatform.modules.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    Optional<PromoCode> findByCodeAndActiveTrue(String code);

    Page<PromoCode> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<PromoCode> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
