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
@Schema(description = "Response containing pending topics and questions for validation")
public class PendingContentResponse {

    @Schema(description = "Pending topics")
    private List<TopicResponse> pendingTopics;

    @Schema(description = "Pending questions")
    private List<QuizQuestionDTO> pendingQuestions;

    @Schema(description = "Total pending topics count")
    private Integer totalTopicsCount;

    @Schema(description = "Total pending questions count")
    private Integer totalQuestionsCount;
}
