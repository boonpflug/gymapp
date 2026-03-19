package com.gymplatform.modules.communication.dto;

import com.gymplatform.modules.communication.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTemplateRequest {
    @NotBlank(message = "Template name is required")
    private String name;
    @NotNull(message = "Channel type is required")
    private ChannelType channelType;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String category;
    private String locale;
    private String logoUrl;
    private String brandColor;
}
