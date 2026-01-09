package com.my.challenger.dto.audio;

import com.my.challenger.entity.enums.AudioChallengeType;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuestionVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create an audio challenge question")
public class CreateAudioQuestionRequest {

    @NotBlank
    @Schema(description = "Question text/instructions", example = "Repeat the rhythm pattern you hear")
    private String question;

    @Schema(description = "Expected answer or description", example = "4/4 time, 120 BPM clap pattern")
    private String answer;

    @NotNull
    @Schema(description = "Type of audio challenge")
    private AudioChallengeType audioChallengeType;

    @Schema(description = "Topic name for categorization")
    private String topic;

    @Schema(description = "Difficulty level")
    private QuizDifficulty difficulty;

    @Schema(description = "Question visibility")
    private QuestionVisibility visibility;

    @Schema(description = "Additional information or hints")
    private String additionalInfo;

    // Audio configuration
    @Schema(description = "Start time of audio segment in seconds")
    private Double audioSegmentStart;

    @Schema(description = "End time of audio segment in seconds")
    private Double audioSegmentEnd;

    @Schema(description = "Minimum score percentage to pass (0-100)")
    private Integer minimumScorePercentage;

    @Schema(description = "BPM for rhythm challenges")
    private Integer rhythmBpm;

    @Schema(description = "Time signature for rhythm challenges")
    private String rhythmTimeSignature;
}
