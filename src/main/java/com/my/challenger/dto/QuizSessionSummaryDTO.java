package com.my.challenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionSummaryDTO {
    private Long sessionId;
    private Integer correctAnswers;
    private Integer totalRounds;
    private Double scorePercentage;
    private String status;
    private String questionSource;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long duration; // in seconds
}
