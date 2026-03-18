package com.gymplatform.modules.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberNoteDto {
    private UUID id;
    private UUID memberId;
    private UUID authorId;
    private String content;
    private Instant createdAt;
}
