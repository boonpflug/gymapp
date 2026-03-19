package com.gymplatform.modules.communication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunicationTemplateRepository extends JpaRepository<CommunicationTemplate, UUID> {

    Page<CommunicationTemplate> findByActiveTrueOrderByNameAsc(Pageable pageable);

    List<CommunicationTemplate> findByChannelTypeAndActiveTrue(ChannelType channelType);

    List<CommunicationTemplate> findByCategoryAndActiveTrue(String category);

    Page<CommunicationTemplate> findByChannelTypeAndActiveTrueOrderByNameAsc(ChannelType channelType, Pageable pageable);
}
