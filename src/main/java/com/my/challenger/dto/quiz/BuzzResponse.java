package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuzzResponse {
    private boolean success;
    private boolean isFirstBuzzer;
    private Instant answerDeadline;
    private String message;
}
