package com.my.challenger.entity.quiz;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_rounds")
public class QuizRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_session_id", nullable = false)
    private QuizSession quizSession;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "team_answer", length = 500)
    private String teamAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect = false;

    @Column(name = "player_who_answered", length = 200)
    private String playerWhoAnswered;

    @Column(name = "discussion_notes", length = 2000)
    private String discussionNotes;

    @Column(name = "round_started_at")
    private LocalDateTime roundStartedAt;

    @Column(name = "discussion_started_at")
    private LocalDateTime discussionStartedAt;

    @Column(name = "answer_submitted_at")
    private LocalDateTime answerSubmittedAt;

    @Column(name = "discussion_duration_seconds")
    private Integer discussionDurationSeconds;

    @Column(name = "total_round_duration_seconds")
    private Integer totalRoundDurationSeconds;

    @Column(name = "hint_used")
    @Builder.Default
    private Boolean hintUsed = false;

    @Column(name = "ai_validation_used")
    @Builder.Default
    private Boolean aiValidationUsed = false;

    @Column(name = "ai_accepted")
    @Builder.Default
    private Boolean aiAccepted = false;

    @Column(name = "ai_confidence", precision = 3, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "ai_explanation", length = 500)
    private String aiExplanation;

    @Column(name = "voice_recording_used")
    @Builder.Default
    private Boolean voiceRecordingUsed = false;

    @Column(name = "ai_feedback", length = 1000)
    private String aiFeedback;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void startRound() {
        this.roundStartedAt = LocalDateTime.now();
    }

    public void startDiscussion() {
        this.discussionStartedAt = LocalDateTime.now();
    }

    public void submitAnswer(String answer, String playerName) {
        this.teamAnswer = answer;
        this.playerWhoAnswered = playerName;
        this.answerSubmittedAt = LocalDateTime.now();
        
        // Calculate durations
        if (this.discussionStartedAt != null && this.answerSubmittedAt != null) {
            this.discussionDurationSeconds = (int) java.time.Duration.between(
                this.discussionStartedAt, this.answerSubmittedAt).getSeconds();
        }
        
        if (this.roundStartedAt != null && this.answerSubmittedAt != null) {
            this.totalRoundDurationSeconds = (int) java.time.Duration.between(
                this.roundStartedAt, this.answerSubmittedAt).getSeconds();
        }
    }
}