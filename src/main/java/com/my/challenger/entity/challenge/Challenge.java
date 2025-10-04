package com.my.challenger.entity.challenge;

import com.my.challenger.entity.ChallengeProgress;
import com.my.challenger.entity.Group;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.ChallengeDifficulty;
import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.ChallengeType;
import com.my.challenger.entity.enums.FrequencyType;
import com.my.challenger.entity.enums.VerificationMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "description", updatable = false, insertable = false)
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
    @Column(name = "verification_method", columnDefinition = "verification_method_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerificationMethod verificationMethod;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VerificationDetails> verificationDetails = new ArrayList<>();

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Stake> stake = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "challenge_status_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ChallengeStatus status = ChallengeStatus.PENDING;

    @Column(name = "quiz_config", columnDefinition = "TEXT")
    private String quizConfig;

    // FIXED: Proper PostgreSQL ENUM handling for difficulty
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, columnDefinition = "challenge_difficulty_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ChallengeDifficulty difficulty = ChallengeDifficulty.MEDIUM;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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