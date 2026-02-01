package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerValidationResult {
    private boolean correct;
    private boolean exactMatch;        // true if Levenshtein/exact matched
    private boolean aiAccepted;        // true if AI accepted as equivalent
    private double aiConfidence;       // AI confidence score (0 if not used)
    private String aiExplanation;      // AI explanation (null if not used)
    private boolean aiUsed;            // whether AI was actually called
}
