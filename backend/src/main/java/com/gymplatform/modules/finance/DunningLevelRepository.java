package com.gymplatform.modules.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DunningLevelRepository extends JpaRepository<DunningLevel, UUID> {
    List<DunningLevel> findAllByOrderByLevelAsc();
}
