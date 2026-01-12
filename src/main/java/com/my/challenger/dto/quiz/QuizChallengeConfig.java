// QuizChallengeConfig.java
package com.my.challenger.dto.quiz;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.ResultSharingPolicy;
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

    // Participation settings
    private Integer maxParticipants;           // null = unlimited
    private Boolean allowOpenEnrollment;       // true = no pre-registration
    private LocalDateTime enrollmentDeadline;

    // Completion settings
    private Boolean individualOnly;            // true = teamBased must be false, each user separate session
    private Integer maxAttempts;               // max attempts per user (default 1)
    private Boolean shuffleQuestions;          // randomize order per participant

    // Result sharing settings
    private ResultSharingPolicy resultSharing; // who can see results
    private Boolean requireResultConsent;      // ask before sharing with creator
}