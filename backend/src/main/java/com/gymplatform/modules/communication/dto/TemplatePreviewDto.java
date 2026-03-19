package com.gymplatform.modules.communication.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplatePreviewDto {
    private String subject;
    private String bodyHtml;
    private String bodyText;
}
