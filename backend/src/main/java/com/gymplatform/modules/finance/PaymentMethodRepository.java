package com.gymplatform.modules.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByMemberIdAndActiveTrue(UUID memberId);
    Optional<PaymentMethod> findByMemberIdAndIsDefaultTrue(UUID memberId);
}
