package com.gymplatform.modules.sales;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.sales.dto.CreateLeadStageRequest;
import com.gymplatform.modules.sales.dto.LeadStageDto;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadStageService {

    private final LeadStageRepository stageRepository;
    private final LeadRepository leadRepository;
    private final AuditLogService auditLogService;

    public List<LeadStageDto> getAll() {
        return stageRepository.findAllByOrderBySortOrderAsc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public LeadStageDto create(CreateLeadStageRequest req, UUID userId) {
        LeadStage stage = LeadStage.builder()
                .name(req.getName())
                .sortOrder(req.getSortOrder())
                .color(req.getColor())
                .isDefault(req.isDefault())
                .isClosed(req.isClosed())
                .isWon(req.isWon())
                .tenantId(TenantContext.getTenantId())
                .build();
        stage = stageRepository.save(stage);
        auditLogService.log("LeadStage", stage.getId(), "CREATE", userId, null, null);
        return toDto(stage);
    }

    @Transactional
    public LeadStageDto update(UUID id, CreateLeadStageRequest req, UUID userId) {
        LeadStage stage = stageRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("LeadStage", id));
        stage.setName(req.getName());
        stage.setSortOrder(req.getSortOrder());
        stage.setColor(req.getColor());
        stage.setDefault(req.isDefault());
        stage.setClosed(req.isClosed());
        stage.setWon(req.isWon());
        stage = stageRepository.save(stage);
        auditLogService.log("LeadStage", stage.getId(), "UPDATE", userId, null, null);
        return toDto(stage);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        long count = leadRepository.countByStageId(id);
        if (count > 0) {
            throw BusinessException.conflict("Cannot delete stage with " + count + " leads. Move them first.");
        }
        stageRepository.deleteById(id);
        auditLogService.log("LeadStage", id, "DELETE", userId, null, null);
    }

    @Transactional
    public void initDefaultStages(UUID userId) {
        if (!stageRepository.findAllByOrderBySortOrderAsc().isEmpty()) return;
        String[][] defaults = {
                {"New", "#3B82F6", "true", "false", "false"},
                {"Contacted", "#F59E0B", "false", "false", "false"},
                {"Trial Booked", "#8B5CF6", "false", "false", "false"},
                {"Proposal Sent", "#EC4899", "false", "false", "false"},
                {"Converted", "#10B981", "false", "true", "true"},
                {"Lost", "#6B7280", "false", "true", "false"},
        };
        for (int i = 0; i < defaults.length; i++) {
            LeadStage s = LeadStage.builder()
                    .name(defaults[i][0]).color(defaults[i][1])
                    .isDefault(Boolean.parseBoolean(defaults[i][2]))
                    .isClosed(Boolean.parseBoolean(defaults[i][3]))
                    .isWon(Boolean.parseBoolean(defaults[i][4]))
                    .sortOrder(i).tenantId(TenantContext.getTenantId()).build();
            stageRepository.save(s);
        }
    }

    private LeadStageDto toDto(LeadStage s) {
        return LeadStageDto.builder()
                .id(s.getId())
                .name(s.getName())
                .sortOrder(s.getSortOrder())
                .color(s.getColor())
                .isDefault(s.isDefault())
                .isClosed(s.isClosed())
                .isWon(s.isWon())
                .leadCount(leadRepository.countByStageId(s.getId()))
                .build();
    }
}
