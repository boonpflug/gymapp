package com.gymplatform.modules.communication.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class PreviewTemplateRequest {
    private UUID templateId;
    private Map<String, String> variables;
}
