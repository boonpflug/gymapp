package com.gymplatform.modules.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassDefinitionRepository extends JpaRepository<ClassDefinition, UUID> {

    Page<ClassDefinition> findByActiveTrue(Pageable pageable);

    List<ClassDefinition> findByCategoryIdAndActiveTrue(UUID categoryId);
}
