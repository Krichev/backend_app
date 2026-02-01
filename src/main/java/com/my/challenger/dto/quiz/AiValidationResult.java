package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiValidationResult {
    private boolean equivalent;
    private double confidence;
    private String explanation;
    private boolean aiUsed;
    private boolean fallbackUsed;
    private long processingTimeMs;
}
