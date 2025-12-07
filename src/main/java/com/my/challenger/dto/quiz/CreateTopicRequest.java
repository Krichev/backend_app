// CreateTopicRequest.java
package com.my.challenger.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new topic")
public class CreateTopicRequest {

    @NotBlank(message = "Topic name is required")
    @Size(min = 2, max = 100, message = "Topic name must be between 2 and 100 characters")
    @Schema(description = "Topic name", example = "Science", required = true)
    private String name;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    @Schema(description = "Topic category", example = "General Knowledge")
    private String category;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Topic description", example = "Questions related to science and technology")
    private String description;

    @Schema(description = "Parent topic ID (for hierarchical structure)", example = "1")
    private Long parentId;
}

