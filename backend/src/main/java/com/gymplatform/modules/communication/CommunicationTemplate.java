package com.gymplatform.modules.communication;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "communication_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunicationTemplate extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(length = 500)
    private String subject;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(length = 100)
    private String category;

    @Column(length = 20)
    private String locale;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "brand_color", length = 7)
    private String brandColor;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;
}
