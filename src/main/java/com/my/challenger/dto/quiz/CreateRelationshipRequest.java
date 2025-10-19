package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.RelationshipType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRelationshipRequest {

    @NotNull(message = "Related user ID is required")
    private Long relatedUserId;

    @NotNull(message = "Relationship type is required")
    private RelationshipType relationshipType;
}
