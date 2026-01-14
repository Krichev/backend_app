package com.my.challenger.entity.challenge;

import com.my.challenger.entity.ChallengeProgress;
import com.my.challenger.entity.Group;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "challenges")
@Getter
@Setter
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "challenge_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ChallengeType type;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Task> tasks = new HashSet<>();

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChallengeProgress> progress = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "is_public")
    private boolean isPublic = true;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private FrequencyType frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "challenge_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ChallengeStatus status = ChallengeStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ChallengeDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", columnDefinition = "verification_method_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerificationMethod verificationMethod;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VerificationDetails> verificationDetails = new ArrayList<>();

    // ========== PAYMENT FIELDS ==========

    @Column(name = "has_entry_fee")
    private boolean hasEntryFee = false;

    @Column(name = "entry_fee_amount", precision = 10, scale = 2)
    private BigDecimal entryFeeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_fee_currency")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CurrencyType entryFeeCurrency;

    @Column(name = "has_prize")
    private boolean hasPrize = false;

    @Column(name = "prize_amount", precision = 10, scale = 2)
    private BigDecimal prizeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "prize_currency")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CurrencyType prizeCurrency;

    @Column(name = "prize_pool", precision = 10, scale = 2)
    private BigDecimal prizePool = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PaymentType paymentType = PaymentType.FREE;

    // ========== ACCESS CONTROL FOR PRIVATE CHALLENGES ==========

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ChallengeAccess> accessList = new ArrayList<>();

    @Column(name = "requires_approval")
    private boolean requiresApproval = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "current_participant_count")
    private Integer currentParticipantCount = 0;

    @Column(name = "allow_open_enrollment")
    private Boolean allowOpenEnrollment = true;

    @Column(name = "enrollment_deadline")
    private LocalDateTime enrollmentDeadline;

    // ========== AUDIO CONFIGURATION FIELDS ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_media_id")
    private MediaFile audioMedia;

    @Column(name = "audio_start_time")
    private Double audioStartTime = 0.0;

    @Column(name = "audio_end_time")
    private Double audioEndTime;

    @Column(name = "minimum_score_percentage")
    private Integer minimumScorePercentage = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (prizePool == null) {
            prizePool = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== HELPER METHODS ==========

    public boolean canAcceptMoreParticipants() {
        if (maxParticipants == null) return true;
        return currentParticipantCount < maxParticipants;
    }

    public boolean isEnrollmentOpen() {
        if (enrollmentDeadline == null) return true;
        return LocalDateTime.now().isBefore(enrollmentDeadline);
    }

    public void addToAccessList(User user) {
        ChallengeAccess access = new ChallengeAccess();
        access.setChallenge(this);
        access.setUser(user);
        access.setGrantedAt(LocalDateTime.now());
        access.setGrantedBy(this.creator);
        this.accessList.add(access);
    }

    public void removeFromAccessList(User user) {
        accessList.removeIf(access -> access.getUser().getId().equals(user.getId()));
    }

    public boolean hasAccess(User user) {
        if (isPublic) return true;
        if (creator.getId().equals(user.getId())) return true;
        return accessList.stream()
                .anyMatch(access -> access.getUser().getId().equals(user.getId()));
    }

    public void addEntryFee(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.prizePool = this.prizePool.add(amount);
        }
    }
}