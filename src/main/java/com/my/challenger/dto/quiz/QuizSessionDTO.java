package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuestionSource;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuizSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionDTO {
    private Long id;
    private Long challengeId;
    private String challengeTitle;
    private Long hostUserId;
    private String hostUsername;
    private String teamName;
    private List<String> teamMembers;
    private QuizDifficulty difficulty;
    private Integer roundTimeSeconds;
    private Integer totalRounds;
    private Integer completedRounds;
    private Integer correctAnswers;
    private Double scorePercentage;
    private Boolean enableAiHost;
    private QuestionSource questionSource;
    private QuizSessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalDurationSeconds;
    private Boolean isPaused;
    private String pauseMetadata;
    private Boolean enableAiAnswerValidation;

    private LocalDateTime createdAt;
    
    // Pause metadata
    private LocalDateTime pausedAt;
    private Integer pausedAtRound;
    private Integer remainingTimeSeconds;
    private String pausedAnswer;
    private String pausedNotes;
}