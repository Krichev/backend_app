package com.my.challenger.dto.vibration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundResultDTO {
    private Long songId;
    private String songTitle;
    private String artist;
    private String selectedAnswer;
    private boolean isCorrect;
    private Integer responseTimeMs;
    private Integer replaysUsed;
    private Integer pointsEarned;
}
