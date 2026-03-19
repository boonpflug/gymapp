package com.gymplatform.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ClassCategoryDto {
    private UUID id;
    private String name;
    private String description;
    private String color;
    private boolean active;
    private Instant createdAt;
}
