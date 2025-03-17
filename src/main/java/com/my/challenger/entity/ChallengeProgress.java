package com.my.challenger.entity;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ProgressStatus;
import com.my.challenger.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @ElementCollection
    @CollectionTable(name = "challenge_progress_completed_days")
    private List<LocalDate> completedDays;

    private Integer streak;

    private Double totalRewardsEarned;
}