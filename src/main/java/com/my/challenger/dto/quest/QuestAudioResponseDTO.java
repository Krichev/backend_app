package com.my.challenger.dto.quest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for quest audio configuration response
 * Contains complete audio configuration including URLs and metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestAudioResponseDTO {

    /**
     * ID of the audio file in media_files table
     */
    private Long audioMediaId;

    /**
     * URL to stream the audio file
     */
    private String audioUrl;

    /**
     * Audio segment start time in seconds
     */
    private Double audioStartTime;

    /**
     * Audio segment end time in seconds (null means full duration)
     */
    private Double audioEndTime;

    /**
     * Total duration of the audio file in seconds
     */
    private Double totalDuration;

    /**
     * Minimum score percentage (0-100) required to complete the quest
     */
    private Integer minimumScorePercentage;
}
