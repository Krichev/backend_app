package com.my.challenger.dto.quiz;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Topic information")
public class TopicResponse {

    @Schema(description = "Topic ID", example = "1")
    private Long id;

    @Schema(description = "Topic name", example = "Science")
    private String name;

    @Schema(description = "Topic category", example = "General Knowledge")
    private String category;

    @Schema(description = "Topic description")
    private String description;

    @Schema(description = "Number of questions in this topic", example = "25")
    private Integer questionCount;

    @Schema(description = "Is topic active", example = "true")
    private Boolean isActive;

    @Schema(description = "Creator user ID")
    private Long creatorId;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
