package com.my.challenger.dto.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeAudioResponseDTO {
    
    private Long audioMediaId;
    private String audioUrl;
    private Double audioStartTime;
    private Double audioEndTime;
    private Double totalDuration;
    private Integer minimumScorePercentage;
}