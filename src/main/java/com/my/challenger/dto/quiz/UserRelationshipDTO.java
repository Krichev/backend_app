package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.RelationshipStatus;
import com.my.challenger.entity.enums.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User relationship information")
public class UserRelationshipDTO {

    @Schema(description = "Relationship ID")
    private Long id;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Related user ID")
    private Long relatedUserId;

    @Schema(description = "Related user username")
    private String relatedUserUsername;

    @Schema(description = "Related user avatar")
    private String relatedUserAvatar;

    @Schema(description = "Relationship type")
    private RelationshipType relationshipType;

    @Schema(description = "Relationship status")
    private RelationshipStatus status;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
