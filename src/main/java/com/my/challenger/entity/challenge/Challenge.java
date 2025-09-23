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

    // Relationship with ChallengeProgress instead of direct participants
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
    private FrequencyType frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method")
    private VerificationMethod verificationMethod;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VerificationDetails> verificationDetails = new ArrayList<>();

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Stake> stake = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status = ChallengeStatus.PENDING;

    @Column(name = "quiz_config", columnDefinition = "TEXT")
    private String quizConfig; // JSON string

    // NEW: Difficulty field
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private ChallengeDifficulty difficulty = ChallengeDifficulty.MEDIUM; // Default to MEDIUM

    // Audit fields
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Set default difficulty if not specified
        if (this.difficulty == null) {
            this.difficulty = ChallengeDifficulty.MEDIUM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Convenience methods for difficulty
    public boolean isEasyDifficulty() {
        return this.difficulty == ChallengeDifficulty.BEGINNER ||
                this.difficulty == ChallengeDifficulty.EASY;
    }

    public boolean isHardDifficulty() {
        return this.difficulty == ChallengeDifficulty.HARD ||
                this.difficulty == ChallengeDifficulty.EXPERT ||
                this.difficulty == ChallengeDifficulty.EXTREME;
    }

    public int getDifficultyLevel() {
        return this.difficulty != null ? this.difficulty.getLevel() : 3; // Default to medium level
    }

    public String getDifficultyDisplayName() {
        return this.difficulty != null ? this.difficulty.getDisplayName() : "Medium";
    }

    public String getDifficultyDescription() {
        return this.difficulty != null ? this.difficulty.getDescription() : "Moderate difficulty";
    }
}