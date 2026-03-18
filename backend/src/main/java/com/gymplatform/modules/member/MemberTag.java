package com.gymplatform.modules.member;

import com.gymplatform.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member_tags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberTag extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String color;
}
