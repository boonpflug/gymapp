package com.gymplatform.modules.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnamneseFormRepository extends JpaRepository<AnamneseForm, UUID> {
    List<AnamneseForm> findByActiveTrueOrderByNameAsc();
    List<AnamneseForm> findAllByOrderByCreatedAtDesc();
}
