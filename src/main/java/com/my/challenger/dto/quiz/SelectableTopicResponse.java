package com.my.challenger.dto.quiz;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.my.challenger.entity.enums.ValidationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Selectable topic with full hierarchy path")
public class SelectableTopicResponse {

    @Schema(description = "Topic ID", example = "1")
    private Long id;

    @Schema(description = "Topic name", example = "Minerals")
    private String name;

    @Schema(description = "Full hierarchical path", example = "Geography > Geology > Minerals")
    private String fullPath;

    @Schema(description = "Depth in hierarchy (0 = root)", example = "2")
    private Integer depth;

    @Schema(description = "Validation status", example = "APPROVED")
    private ValidationStatus validationStatus;

    @Schema(description = "True if current user created this topic", example = "false")
    private Boolean isOwn;
}
