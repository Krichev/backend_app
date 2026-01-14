package com.my.challenger.dto.challenge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeAudioConfigDTO {
    
    private Long audioMediaId;
    
    @Min(value = 0, message = "Start time must be >= 0")
    private Double audioStartTime;
    
    private Double audioEndTime;
    
    @Min(value = 0, message = "Minimum score must be >= 0")
    @Max(value = 100, message = "Minimum score must be <= 100")
    private Integer minimumScorePercentage;
}