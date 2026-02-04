package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrainRingAnswerResponse {
    private boolean isCorrect;
    private boolean playerLockedOut;
    private boolean roundComplete;
    private String correctAnswer;
    private boolean nextBuzzerAllowed;
    private Long winnerUserId;
}
