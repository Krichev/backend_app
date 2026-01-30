package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PauseQuizSessionRequest {
    private Integer pausedAtRound;
    private Integer remainingTimeSeconds;
    private String currentAnswer;
    private String discussionNotes;
}
