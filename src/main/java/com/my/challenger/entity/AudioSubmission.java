package com.my.challenger.entity;

import com.my.challenger.entity.quiz.QuizQuestion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audio_submissions")
public class AudioSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Audio storage
    @Column(name = "user_audio_s3_key", nullable = false)
    private String userAudioS3Key;

    @Column(name = "user_audio_bucket", nullable = false)
    private String userAudioBucket;

    @Column(name = "submission_media_id")
    private Long submissionMediaId;

    // Processing status
    @Column(name = "processing_status", nullable = false)
    private String processingStatus = "PENDING";

    @Column(name = "processing_progress")
    private Integer processingProgress = 0;

    @Column(name = "error_message")
    private String errorMessage;

    // Scoring results (populated on COMPLETED)
    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "pitch_score")
    private Double pitchScore;

    @Column(name = "rhythm_score")
    private Double rhythmScore;

    @Column(name = "voice_score")
    private Double voiceScore;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "minimum_score_required")
    private Integer minimumScoreRequired;

    @Column(name = "detailed_metrics")
    @JdbcTypeCode(SqlTypes.JSON)
    private String detailedMetrics;

    // Challenge context (denormalized from question for scoring)
    @Column(name = "challenge_type", nullable = false)
    private String challengeType;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public boolean isProcessed() {
        return "COMPLETED".equals(processingStatus) || "FAILED".equals(processingStatus);
    }

    public void calculatePassed() {
        if (overallScore != null && minimumScoreRequired != null) {
            this.passed = overallScore >= minimumScoreRequired;
        } else {
            this.passed = false;
        }
    }
}
