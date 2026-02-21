package com.my.challenger.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResult {
    private boolean correct;
    private String message;         // "Correct!", "Try again", etc.
    private int score;              // Points awarded
    private int rank;               // Current rank among participants
}
