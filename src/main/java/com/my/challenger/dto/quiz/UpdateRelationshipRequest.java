package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRelationshipRequest {
    private RelationshipType relationshipType;
    private String nickname;
    private String notes;
    private Boolean isFavorite;
}
