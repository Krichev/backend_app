package com.my.challenger.entity;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ProgressStatus;
import com.my.challenger.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ChallengeProgress entity with proper PostgreSQL ENUM type handling
 *
 * Tracks user progress on challenges including completion status,
 * verification status, and completion percentage
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "challenge_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"challenge_id", "user_id"}))
public class ChallengeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Progress status - properly mapped to PostgreSQL ENUM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "progress_status_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private ProgressStatus status = ProgressStatus.IN_PROGRESS;

    /**
     * Completion percentage (0.0 to 100.0)
     */
    @Column(name = "completion_percentage")
    @Builder.Default
    private Double completionPercentage = 0.0;

    /**
     * JSON or URL to verification data
     */
    @Column(name = "verification_data", columnDefinition = "TEXT")
    private String verificationData;

    /**
     * Verification status - properly mapped to PostgreSQL ENUM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", columnDefinition = "verification_status_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /**
     * User who verified this progress
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    /**
     * When the progress was verified
     */
    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    /**
     * Current streak count (consecutive days/periods completed)
     */
    @Column(name = "streak")
    @Builder.Default
    private Integer streak = 0;

    /**
     * Total rewards earned from this challenge
     */
    @Column(name = "total_rewards_earned")
    @Builder.Default
    private Double totalRewardsEarned = 0.0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Collection of dates when tasks were completed
     * Stored in a separate table for better normalization
     */
    @ElementCollection
    @CollectionTable(
            name = "challenge_progress_completed_days",
            joinColumns = @JoinColumn(name = "challenge_progress_id")
    )
    @Column(name = "completed_day")
    @Builder.Default
    private List<LocalDate> completedDays = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Set defaults if null
        if (status == null) {
            status = ProgressStatus.IN_PROGRESS;
        }
        if (verificationStatus == null) {
            verificationStatus = VerificationStatus.PENDING;
        }
        if (completionPercentage == null) {
            completionPercentage = 0.0;
        }
        if (streak == null) {
            streak = 0;
        }
        if (totalRewardsEarned == null) {
            totalRewardsEarned = 0.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if challenge is completed
     */
    public boolean isCompleted() {
        return status == ProgressStatus.COMPLETED;
    }

    /**
     * Check if progress is verified
     */
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    /**
     * Check if challenge is in progress
     */
    public boolean isInProgress() {
        return status == ProgressStatus.IN_PROGRESS;
    }

    /**
     * Check if verification is pending
     */
    public boolean isVerificationPending() {
        return verificationStatus == VerificationStatus.PENDING;
    }

    /**
     * Update completion percentage and check if completed
     */
    public void updateProgress(double percentage) {
        this.completionPercentage = Math.min(100.0, Math.max(0.0, percentage));

        if (this.completionPercentage >= 100.0) {
            this.status = ProgressStatus.COMPLETED;
        }
    }

    /**
     * Mark a day as completed
     */
    public void markDayCompleted(LocalDate date) {
        if (!completedDays.contains(date)) {
            completedDays.add(date);
        }
    }

    /**
     * Verify the progress
     */
    public void verify(User verifier) {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedBy = verifier;
        this.verificationDate = LocalDateTime.now();
    }

    /**
     * Reject the verification
     */
    public void reject(User verifier) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verifiedBy = verifier;
        this.verificationDate = LocalDateTime.now();
    }

    /**
     * Add rewards earned
     */
    public void addRewards(double rewards) {
        this.totalRewardsEarned = (this.totalRewardsEarned != null ? this.totalRewardsEarned : 0.0) + rewards;
    }

    /**
     * Increment streak
     */
    public void incrementStreak() {
        this.streak = (this.streak != null ? this.streak : 0) + 1;
    }

    /**
     * Reset streak
     */
    public void resetStreak() {
        this.streak = 0;
    }
}