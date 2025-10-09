package com.my.challenger.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a topic")
public class UpdateTopicRequest {

    @Size(min = 2, max = 100, message = "Topic name must be between 2 and 100 characters")
    @Schema(description = "Topic name", example = "Science")
    private String name;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    @Schema(description = "Topic category", example = "General Knowledge")
    private String category;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Topic description")
    private String description;

    @Schema(description = "Is topic active", example = "true")
    private Boolean isActive;
}
