package com.my.challenger.dto.audio;

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
@Schema(description = "Audio challenge submission with scoring results")
public class AudioChallengeSubmissionDTO {

    @Schema(description = "Submission ID")
    private Long id;

    @Schema(description = "Question ID")
    private Long questionId;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Processing status: PENDING, PROCESSING, COMPLETED, FAILED")
    private String processingStatus;

    @Schema(description = "Processing progress (0-100)")
    private Integer processingProgress;

    @Schema(description = "Overall score (0-100)")
    private Double overallScore;

    @Schema(description = "Pitch accuracy score (0-100)")
    private Double pitchScore;

    @Schema(description = "Rhythm timing score (0-100)")
    private Double rhythmScore;

    @Schema(description = "Voice similarity score (0-100)")
    private Double voiceScore;

    @Schema(description = "Whether the user passed the challenge")
    private Boolean passed;

    @Schema(description = "Minimum score required to pass")
    private Integer minimumScoreRequired;

    @Schema(description = "Detailed scoring metrics as JSON")
    private String detailedMetrics;

    @Schema(description = "When the submission was created")
    private LocalDateTime createdAt;

    @Schema(description = "When processing completed")
    private LocalDateTime processedAt;
}
