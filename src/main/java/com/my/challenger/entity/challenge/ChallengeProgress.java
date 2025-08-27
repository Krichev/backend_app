//package com.my.challenger.entity.challenge;
//
//import com.my.challenger.entity.User;
//import com.my.challenger.entity.challenge.Challenge;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import lombok.NoArgsConstructor;
//import lombok.AllArgsConstructor;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "challenge_progress")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class ChallengeProgress {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "challenge_id", nullable = false)
//    private Challenge challenge;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @Column(name = "status", nullable = false)
//    @Enumerated(EnumType.STRING)
//    private ProgressStatus status = ProgressStatus.IN_PROGRESS;
//
//    @Column(name = "completion_percentage")
//    private Double completionPercentage = 0.0;
//
//    @Column(name = "verification_data", columnDefinition = "TEXT")
//    private String verificationData; // JSON or URL to verification data
//
//    @Column(name = "verification_status")
//    @Enumerated(EnumType.STRING)
//    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "verified_by")
//    private User verifiedBy;
//
//    @Column(name = "verification_date")
//    private LocalDateTime verificationDate;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt = LocalDateTime.now();
//
//    // Update timestamp before update
//    @PreUpdate
//    public void preUpdate() {
//        this.updatedAt = LocalDateTime.now();
//    }
//
//    // Enums for status values
//    public enum ProgressStatus {
//        IN_PROGRESS,
//        COMPLETED,
//        FAILED
//    }
//
//    public enum VerificationStatus {
//        PENDING,
//        VERIFIED,
//        REJECTED
//    }
//}