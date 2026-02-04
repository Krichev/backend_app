// src/main/java/com/my/challenger/entity/quiz/QuizSession.java
package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.GameMode;
import com.my.challenger.entity.enums.QuestionSource;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuizSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_sessions")
public class QuizSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "host_user_id", nullable = false)
    private User hostUser;

    @Column(name = "team_name", length = 200)
    private String teamName;

    @Column(name = "team_members", length = 1000)
    private String teamMembers; // JSON array of team member names

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, columnDefinition = "quiz_difficulty")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuizDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false, columnDefinition = "game_mode")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private GameMode gameMode = GameMode.STANDARD;

    @Column(name = "answer_time_seconds")
    @Builder.Default
    private Integer answerTimeSeconds = 20;

    @Column(name = "round_time_seconds")
    private Integer roundTimeSeconds;

    @Column(name = "total_rounds")
    private Integer totalRounds;

    @Column(name = "completed_rounds")
    private Integer completedRounds = 0;

    @Column(name = "correct_answers")
    private Integer correctAnswers = 0;

    @Column(name = "enable_ai_host")
    private Boolean enableAiHost = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_source", nullable = false, columnDefinition = "question_source")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuestionSource questionSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "quiz_session_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private QuizSessionStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;

    @Column(name = "enable_ai_answer_validation")
    @Builder.Default
    private Boolean enableAiAnswerValidation = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "paused_at_round")
    private Integer pausedAtRound;

    @Column(name = "remaining_time_seconds")
    private Integer remainingTimeSeconds;

    @Column(name = "paused_answer")
    private String pausedAnswer;

    @Column(name = "paused_notes")
    private String pausedNotes;

    // Bi-directional relationship with quiz rounds
    @OneToMany(mappedBy = "quizSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizRound> rounds = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = QuizSessionStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void startSession() {
        this.status = QuizSessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void completeSession() {
        this.status = QuizSessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.totalDurationSeconds = (int) java.time.Duration.between(this.startedAt, this.completedAt).getSeconds();
        }
    }

    public double getScorePercentage() {
        if (totalRounds == null || totalRounds == 0) {
            return 0.0;
        }
        return (correctAnswers.doubleValue() / totalRounds.doubleValue()) * 100.0;
    }

    /**
     * Archive the quiz session (can only archive completed sessions)
     */
    public void archiveSession() {
        if (this.status != QuizSessionStatus.COMPLETED) {
            throw new IllegalStateException("Can only archive completed quiz sessions");
        }
        this.status = QuizSessionStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }
}