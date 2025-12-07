package com.my.challenger.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to move a topic to a new parent")
public class MoveTopicRequest {

    @Schema(description = "New parent topic ID (null to move to root)", example = "1", nullable = true)
    private Long newParentId;
}
