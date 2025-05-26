// src/main/java/com/my/challenger/entity/quiz/QuizSession.java
package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.enums.QuizSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @ManyToOne
    @JoinColumn(name = "host_user_id", nullable = false)
    private User hostUser;

    @Column(name = "team_name", length = 200)
    private String teamName;

    @Column(name = "team_members", length = 1000)
    private String teamMembers; // JSON array of team member names

    @Enumerated(EnumType.STRING)
    private QuizDifficulty difficulty;

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

    @Column(name = "question_source")
    private String questionSource; // 'app' or 'user'

    @Enumerated(EnumType.STRING)
    private QuizSessionStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_duration_seconds")
    private Integer totalDurationSeconds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
}