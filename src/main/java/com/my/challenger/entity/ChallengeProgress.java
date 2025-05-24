package com.my.challenger.entity;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ProgressStatus;
import com.my.challenger.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "challenge_progress")
public class ChallengeProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status;

    @Column(name = "completion_percentage")
    private Double completionPercentage = 0.0;

    @Column(name = "verification_data")
    private String verificationData;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus;

    @ManyToOne
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // The following fields need to be added to your schema if you want to keep them
    // Or you can remove them from the entity if you don't need them

    // Option 1: Create a separate table for completed days (recommended)
    @ElementCollection
    @CollectionTable(
            name = "challenge_progress_completed_days",
            joinColumns = @JoinColumn(name = "challenge_progress_id")
    )
    @Column(name = "completed_day")
    private List<LocalDate> completedDays = new ArrayList<>();

    // Option 2: Add these columns to your challenge_progress table
    @Column(name = "streak")
    private Integer streak;

    @Column(name = "total_rewards_earned")
    private Double totalRewardsEarned;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}