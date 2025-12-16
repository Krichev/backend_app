package com.my.challenger.dto.quest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for quest audio configuration request
 * Contains audio segment times and minimum score requirement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestAudioConfigDTO {

    /**
     * ID of the audio file in media_files table
     */
    private Long audioMediaId;

    /**
     * Audio segment start time in seconds (default: 0)
     */
    @Min(value = 0, message = "Audio start time must be >= 0")
    private Double audioStartTime;

    /**
     * Audio segment end time in seconds (null means full duration)
     */
    @Min(value = 0, message = "Audio end time must be >= 0")
    private Double audioEndTime;

    /**
     * Minimum score percentage (0-100) required to complete the quest
     */
    @Min(value = 0, message = "Minimum score percentage must be >= 0")
    @Max(value = 100, message = "Minimum score percentage must be <= 100")
    private Integer minimumScorePercentage;
}
