package com.my.challenger.dto.quiz;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.my.challenger.entity.enums.ValidationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    @Schema(description = "Original name (if translated)")
    private String originalName;

    @Schema(description = "Original description (if translated)")
    private String originalDescription;

    @Schema(description = "Number of questions in this topic", example = "25")
    private Integer questionCount;

    @Schema(description = "Is topic active", example = "true")
    private Boolean isActive;

    @Schema(description = "Creator user ID")
    private Long creatorId;

    @Schema(description = "Parent topic ID")
    private Long parentId;

    @Schema(description = "Parent topic name")
    private String parentName;

    @Schema(description = "Materialized path")
    private String path;

    @Schema(description = "Depth in hierarchy (0 = root)")
    private Integer depth;

    @Schema(description = "Is this a system-generated topic")
    private Boolean isSystemTopic;

    @Schema(description = "Validation status")
    private ValidationStatus validationStatus;

    @Schema(description = "URL-friendly slug")
    private String slug;

    @Schema(description = "Number of child topics")
    private Integer childCount;

    @Schema(description = "Child topics (for tree responses)")
    private List<TopicResponse> children;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "Validation timestamp")
    private LocalDateTime validatedAt;
}