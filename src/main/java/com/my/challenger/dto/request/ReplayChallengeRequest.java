package com.my.challenger.dto.request;

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
public class ReplayChallengeRequest {
    private String difficulty;
    
    @Min(1)
    @Max(100)
    private Integer roundCount;
    
    @Min(5)
    @Max(300)
    private Integer roundTime;
    
    private Boolean enableAIHost;
    private Boolean enableAiAnswerValidation;
    private String questionSource;
}
