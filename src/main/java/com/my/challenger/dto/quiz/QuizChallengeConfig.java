// QuizChallengeConfig.java
package com.my.challenger.dto.quiz;
import com.my.challenger.entity.enums.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizChallengeConfig {
    // Existing fields
    private QuizDifficulty defaultDifficulty;
    private Integer defaultRoundTimeSeconds;
    private Integer defaultTotalRounds;
    private Boolean enableAiHost;
    private String questionSource;
    private Boolean allowCustomQuestions;

    // NEW: Missing fields from frontend
    private String gameType;  // e.g., "WWW"
    private String teamName;  // Team name for quiz
    private List<String> teamMembers;  // List of team member names
    private Boolean teamBased;  // Whether it's team-based
}