// QuizChallengeConfig.java
package com.my.challenger.dto.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizChallengeConfig {
    private QuizDifficulty defaultDifficulty;
    private Integer defaultRoundTimeSeconds;
    private Integer defaultTotalRounds;
    private Boolean enableAiHost;
    private String questionSource;
    private Boolean allowCustomQuestions;
}