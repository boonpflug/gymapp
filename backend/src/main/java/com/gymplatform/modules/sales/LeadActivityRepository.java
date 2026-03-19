package com.gymplatform.modules.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {

    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(UUID leadId);

    @Query("SELECT a FROM LeadActivity a WHERE a.activityType = 'TASK' AND a.completedAt IS NULL " +
            "AND a.dueDate <= :date ORDER BY a.dueDate ASC")
    List<LeadActivity> findOverdueTasks(@Param("date") LocalDate date);

    @Query("SELECT a FROM LeadActivity a WHERE a.activityType = 'TASK' AND a.completedAt IS NULL " +
            "AND a.staffId = :staffId ORDER BY a.dueDate ASC")
    List<LeadActivity> findPendingTasksByStaff(@Param("staffId") UUID staffId);
}
