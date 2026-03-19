package com.gymplatform.modules.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRuleRepository extends JpaRepository<NotificationRule, UUID> {

    List<NotificationRule> findByTriggerEventAndActiveTrue(TriggerEvent triggerEvent);

    List<NotificationRule> findByActiveTrueOrderByNameAsc();

    List<NotificationRule> findByTemplateId(UUID templateId);
}
