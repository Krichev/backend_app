package com.my.challenger.dto.quiz;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Topic tree structure response")
public class TopicTreeResponse {

    @Schema(description = "Topic ID", example = "1")
    private Long id;

    @Schema(description = "Topic name", example = "Science")
    private String name;

    @Schema(description = "URL-friendly slug")
    private String slug;

    @Schema(description = "Number of questions in this topic", example = "25")
    private Integer questionCount;

    @Schema(description = "Child topics")
    private List<TopicTreeResponse> children;
}
