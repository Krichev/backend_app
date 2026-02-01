package com.my.challenger.entity.penalty;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.PenaltyStatus;
import com.my.challenger.entity.enums.PenaltyType;
import com.my.challenger.entity.enums.PenaltyVerificationMethod;
import com.my.challenger.entity.wager.Wager;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "penalties")
public class Penalty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wager_id")
    private Wager wager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id", nullable = false)
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PenaltyType penaltyType;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private PenaltyStatus status = PenaltyStatus.PENDING;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PenaltyVerificationMethod verificationMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proof_media_id")
    private MediaFile proofMedia;

    @Column(name = "proof_description", columnDefinition = "TEXT")
    private String proofDescription;

    @Column(name = "screen_time_minutes")
    private Integer screenTimeMinutes;

    @Column(name = "point_amount")
    private Long pointAmount;

    @Column(name = "appeal_reason", columnDefinition = "TEXT")
    private String appealReason;

    @Column(name = "appealed_at")
    private LocalDateTime appealedAt;

    @Column(name = "escalation_applied")
    @Builder.Default
    private Boolean escalationApplied = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
