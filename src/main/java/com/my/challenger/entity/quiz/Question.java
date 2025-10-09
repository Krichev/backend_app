package com.my.challenger.entity.quiz;

import com.my.challenger.entity.enums.QuizDifficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_questions",
        indexes = {
                @Index(name = "idx_tournament_id", columnList = "tournament_id"),
                @Index(name = "idx_quiz_question_id", columnList = "quiz_question_id"),
                @Index(name = "idx_tournament_order", columnList = "tournament_id, display_order"),
                @Index(name = "idx_is_active", columnList = "is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tournament_display_order",
                        columnNames = {"tournament_id", "display_order"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Required reference to question bank
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_question_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tournament_question_quiz_question"))
    private QuizQuestion quizQuestion;

    // Tournament context
    @Column(name = "tournament_id", nullable = false)
    private Integer tournamentId;

    @Column(name = "tournament_title", nullable = false, columnDefinition = "TEXT")
    private String tournamentTitle;

    // NEW: Auto-generated sequential order per tournament
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    // Optional: Keep legacy question_num for reference only
    @Column(name = "legacy_question_num")
    private Integer legacyQuestionNum;

    // Tournament-specific metadata
    @Column(name = "tournament_type", columnDefinition = "TEXT")
    private String tournamentType;

    @Column(name = "topic_num")
    private Integer topicNum;

    @Column(name = "notices", columnDefinition = "TEXT")
    private String notices;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @Column(name = "rating")
    private Integer rating;

    // Tournament-specific overrides (optional)
    @Column(name = "custom_question", columnDefinition = "TEXT")
    private String customQuestion;

    @Column(name = "custom_answer", columnDefinition = "TEXT")
    private String customAnswer;

    @Column(name = "custom_sources", columnDefinition = "TEXT")
    private String customSources;

    // Tournament-specific settings
    @Column(name = "points")
    @Builder.Default
    private Integer points = 10;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "is_bonus_question")
    @Builder.Default
    private Boolean isBonusQuestion = false;

    @Column(name = "is_mandatory")
    @Builder.Default
    private Boolean isMandatory = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Audit fields
    @Column(name = "entered_date", nullable = false, updatable = false)
    private LocalDateTime enteredDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "added_by")
    private Long addedBy;

    @PrePersist
    protected void onCreate() {
        if (enteredDate == null) {
            enteredDate = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =============== HELPER METHODS ===============

    /**
     * Get effective question text (custom override or from bank)
     */
    public String getEffectiveQuestion() {
        return hasCustomQuestion() ? customQuestion : quizQuestion.getQuestion();
    }

    /**
     * Get effective answer text (custom override or from bank)
     */
    public String getEffectiveAnswer() {
        return hasCustomAnswer() ? customAnswer : quizQuestion.getAnswer();
    }

    /**
     * Get effective sources
     */
    public String getEffectiveSources() {
        return hasCustomSources() ? customSources : quizQuestion.getSource();
    }

    public boolean hasCustomQuestion() {
        return customQuestion != null && !customQuestion.trim().isEmpty();
    }

    public boolean hasCustomAnswer() {
        return customAnswer != null && !customAnswer.trim().isEmpty();
    }

    public boolean hasCustomSources() {
        return customSources != null && !customSources.trim().isEmpty();
    }

    public boolean hasAnyCustomizations() {
        return hasCustomQuestion() || hasCustomAnswer() || hasCustomSources();
    }

    /**
     * Check if question has media from question bank
     */
    public boolean hasMedia() {
        return quizQuestion != null && quizQuestion.hasMedia();
    }

    /**
     * Get media URL from question bank
     */
    public String getMediaUrl() {
        return quizQuestion != null ? quizQuestion.getQuestionMediaUrl() : null;
    }

    /**
     * Get difficulty from question bank
     */
    public QuizDifficulty getDifficulty() {
        return quizQuestion != null ? quizQuestion.getDifficulty() : null;
    }

    /**
     * Get topic from question bank
     */
    public String getTopic() {
        return quizQuestion != null ? quizQuestion.getTopic().getName() : null;
    }
}