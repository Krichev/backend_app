package com.my.challenger.entity;

import com.my.challenger.entity.quiz.QuizQuestion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audio_challenge_submissions")
public class AudioChallengeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "submission_audio_path", nullable = false)
    private String submissionAudioPath;

    @Column(name = "submission_media_id")
    private Long submissionMediaId;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus = "PENDING";

    @Column(name = "processing_progress")
    private Integer processingProgress = 0;

    // Scoring results
    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "pitch_score")
    private Double pitchScore;

    @Column(name = "rhythm_score")
    private Double rhythmScore;

    @Column(name = "voice_score")
    private Double voiceScore;

    @Column(name = "detailed_metrics", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String detailedMetrics;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isProcessed() {
        return "COMPLETED".equals(processingStatus) || "FAILED".equals(processingStatus);
    }

    public boolean isPassed() {
        if (overallScore == null || question == null) {
            return false;
        }
        Integer minScore = question.getMinimumScorePercentage();
        return overallScore >= (minScore != null ? minScore : 60);
    }
}
