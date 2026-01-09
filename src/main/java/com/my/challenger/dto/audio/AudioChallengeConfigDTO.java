package com.my.challenger.dto.audio;

import com.my.challenger.entity.enums.AudioChallengeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Configuration for audio challenge questions")
public class AudioChallengeConfigDTO {

    @NotNull
    @Schema(description = "Type of audio challenge", example = "SINGING")
    private AudioChallengeType audioChallengeType;

    @Schema(description = "ID of reference audio file (required for RHYTHM_REPEAT, SOUND_MATCH, SINGING)")
    private Long audioReferenceMediaId;

    @Min(0)
    @Schema(description = "Start time of audio segment in seconds", example = "0.0")
    private Double audioSegmentStart = 0.0;

    @Schema(description = "End time of audio segment in seconds (null = full duration)")
    private Double audioSegmentEnd;

    @Min(0)
    @Max(100)
    @Schema(description = "Minimum score percentage to pass (0-100)", example = "60")
    private Integer minimumScorePercentage = 60;

    @Min(40)
    @Max(240)
    @Schema(description = "BPM for rhythm challenges", example = "120")
    private Integer rhythmBpm;

    @Schema(description = "Time signature for rhythm challenges", example = "4/4")
    private String rhythmTimeSignature;

    @Schema(description = "Additional JSON configuration")
    private String additionalConfig;
}
